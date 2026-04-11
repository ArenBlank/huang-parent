param(
    [string]$BaseUrl = "http://localhost",
    [string]$HostHeader = "app.localhost",
    [long]$ScheduleId = 2,
    [int]$Concurrency = 20,
    [string]$Account = "root_member",
    [string]$Password = "root",
    [string[]]$CredentialPairs,
    [string]$RedisContainer = "redis-container-huang",
    [switch]$SkipRedisCleanup,
    [int]$RetryCount = 3,
    [int]$RetryDelayMs = 200
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Normalize-HeaderValue {
    param($Value)
    if ($null -eq $Value) {
        return $null
    }
    if ($Value -is [System.Array]) {
        return [string]$Value[0]
    }
    return [string]$Value
}

function ConvertTo-JsonBody {
    param([object]$Body)
    if ($null -eq $Body) {
        return $null
    }
    if ($Body -is [string]) {
        return $Body
    }
    return $Body | ConvertTo-Json -Depth 10 -Compress
}

function Invoke-JsonApi {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Url,
        [object]$Body,
        [hashtable]$Headers
    )

    $invokeParams = @{
        Method      = $Method
        Uri         = $Url
        ContentType = "application/json"
    }
    if ($Headers) {
        $invokeParams.Headers = $Headers
    }
    if ($null -ne $Body) {
        $invokeParams.Body = ConvertTo-JsonBody -Body $Body
    }
    return Invoke-RestMethod @invokeParams
}

function New-AppHeaders {
    param([string]$Token)
    $headers = @{}
    if ($HostHeader) {
        $headers["Host"] = $HostHeader
    }
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    return $headers
}

function Login-App {
    param(
        [Parameter(Mandatory = $true)][string]$LoginAccount,
        [Parameter(Mandatory = $true)][string]$LoginPassword
    )

    $response = Invoke-JsonApi -Method "POST" -Url "$BaseUrl/app/auth/login" -Headers (New-AppHeaders) -Body @{
        account   = $LoginAccount
        password  = $LoginPassword
        loginType = "password"
    }

    if ($null -eq $response -or $response.code -ne 200) {
        $message = if ($response) { $response.message } else { "<null>" }
        throw "App login failed for [$LoginAccount], code=$($response.code), message=$message"
    }
    if ($null -eq $response.data -or $null -eq $response.data.userInfo -or $null -eq $response.data.accessToken) {
        throw "App login response for [$LoginAccount] is missing user/token payload"
    }

    return [pscustomobject]@{
        Account = $LoginAccount
        UserId  = [long]$response.data.userInfo.id
        Token   = [string]$response.data.accessToken
    }
}

function Clear-GuardKeys {
    param(
        [Parameter(Mandatory = $true)][long]$UserId,
        [Parameter(Mandatory = $true)][long]$TargetScheduleId
    )

    if ($SkipRedisCleanup) {
        Write-Host ("[INFO] Skip Redis key cleanup for userId={0}" -f $UserId) -ForegroundColor DarkYellow
        return
    }

    $limitKey = "app:limit:course:enroll:$UserId"
    $idemKey = "app:idem:course:enroll:$UserId`:$TargetScheduleId"

    try {
        $null = docker exec $RedisContainer redis-cli DEL $limitKey $idemKey 2>$null
        Write-Host ("[INFO] Redis guard keys cleared for userId={0}" -f $UserId) -ForegroundColor DarkGray
    } catch {
        Write-Warning ("Failed to clear Redis guard keys for userId={0}: {1}" -f $UserId, $_.Exception.Message)
    }
}

function Start-EnrollJob {
    param(
        [Parameter(Mandatory = $true)][int]$Index,
        [Parameter(Mandatory = $true)][string]$RequestAccount,
        [Parameter(Mandatory = $true)][string]$RequestToken
    )

    $headers = New-AppHeaders -Token $RequestToken
    $body = ConvertTo-JsonBody -Body @{ scheduleId = $ScheduleId }

    return Start-Job -ScriptBlock {
        param($Url, $Headers, $Body, $Index, $RequestAccount, $RetryCount, $RetryDelayMs)

        function Normalize-HeaderValue {
            param($Value)
            if ($null -eq $Value) {
                return $null
            }
            if ($Value -is [System.Array]) {
                return [string]$Value[0]
            }
            return [string]$Value
        }

        function Convert-ResponseContent {
            param([string]$Content)
            if ([string]::IsNullOrWhiteSpace($Content)) {
                return $null
            }
            try {
                return $Content | ConvertFrom-Json
            } catch {
                return $Content
            }
        }

        $lastError = $null
        for ($attempt = 1; $attempt -le $RetryCount; $attempt++) {
            try {
                $response = Invoke-WebRequest -Uri $Url -Method Post -Headers $Headers -Body $Body -ContentType "application/json" -UseBasicParsing
                $payload = Convert-ResponseContent -Content $response.Content
                return [pscustomobject]@{
                    Index      = $Index
                    Account    = $RequestAccount
                    HttpStatus = [int]$response.StatusCode
                    BizCode    = if ($payload -is [pscustomobject] -and $null -ne $payload.code) { [int]$payload.code } else { $null }
                    Message    = if ($payload -is [pscustomobject]) { [string]$payload.message } else { [string]$payload }
                    Instance   = Normalize-HeaderValue -Value $response.Headers["X-App-Instance"]
                    Error      = $null
                }
            } catch {
                $lastError = $_
                $webResponse = $_.Exception.Response
                if ($null -ne $webResponse) {
                    try {
                        $stream = $webResponse.GetResponseStream()
                        $reader = New-Object System.IO.StreamReader($stream)
                        $content = $reader.ReadToEnd()
                        $reader.Dispose()
                        $payload = Convert-ResponseContent -Content $content
                        return [pscustomobject]@{
                            Index      = $Index
                            Account    = $RequestAccount
                            HttpStatus = [int]$webResponse.StatusCode
                            BizCode    = if ($payload -is [pscustomobject] -and $null -ne $payload.code) { [int]$payload.code } else { $null }
                            Message    = if ($payload -is [pscustomobject]) { [string]$payload.message } else { [string]$payload }
                            Instance   = Normalize-HeaderValue -Value $webResponse.Headers["X-App-Instance"]
                            Error      = $_.Exception.Message
                        }
                    } catch {
                    }
                }

                if ($attempt -lt $RetryCount) {
                    Start-Sleep -Milliseconds $RetryDelayMs
                }
            }
        }

        return [pscustomobject]@{
            Index      = $Index
            Account    = $RequestAccount
            HttpStatus = -1
            BizCode    = $null
            Message    = $null
            Instance   = $null
            Error      = $lastError.Exception.Message
        }
    } -ArgumentList "$BaseUrl/app/course/enroll", $headers, $body, $Index, $RequestAccount, $RetryCount, $RetryDelayMs
}

$sessions = @()
if ($CredentialPairs -and $CredentialPairs.Count -gt 0) {
    Write-Step "Login provided app accounts for multi-user burst"
    foreach ($pair in $CredentialPairs) {
        if ([string]::IsNullOrWhiteSpace($pair) -or -not $pair.Contains(":")) {
            throw "Invalid credential pair [$pair], expected account:password"
        }
        $segments = $pair.Split(":", 2)
        $sessions += Login-App -LoginAccount $segments[0] -LoginPassword $segments[1]
    }
} else {
    Write-Step "Login app account for single-user idempotency test"
    $sessions += Login-App -LoginAccount $Account -LoginPassword $Password
}

foreach ($session in $sessions) {
    Clear-GuardKeys -UserId $session.UserId -TargetScheduleId $ScheduleId
}

$requests = @()
if ($sessions.Count -eq 1) {
    for ($i = 1; $i -le $Concurrency; $i++) {
        $requests += [pscustomobject]@{
            Index   = $i
            Account = $sessions[0].Account
            Token   = $sessions[0].Token
        }
    }
} else {
    $index = 1
    foreach ($session in $sessions) {
        $requests += [pscustomobject]@{
            Index   = $index
            Account = $session.Account
            Token   = $session.Token
        }
        $index++
    }
}

Write-Step ("Start concurrent enroll test: scheduleId={0}, requestCount={1}, distinctUsers={2}" -f $ScheduleId, $requests.Count, $sessions.Count)

$jobs = @()
foreach ($request in $requests) {
    $jobs += Start-EnrollJob -Index $request.Index -RequestAccount $request.Account -RequestToken $request.Token
}

$results = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job -Force | Out-Null

$sortedResults = $results | Sort-Object Index

Write-Host ""
Write-Host "Per request result:" -ForegroundColor Cyan
$sortedResults | ForEach-Object {
    $msg = if ($_.Error) { $_.Error } elseif ($_.Message) { $_.Message } else { "<empty>" }
    Write-Host ("#{0} account={1} http={2} code={3} instance={4} message={5}" -f $_.Index, $_.Account, $_.HttpStatus, $_.BizCode, $_.Instance, $msg)
}

Write-Host ""
Write-Host "Summary by HTTP status:" -ForegroundColor Cyan
$sortedResults |
    Group-Object HttpStatus |
    Sort-Object Name |
    ForEach-Object { Write-Host ("{0} -> {1}" -f $_.Name, $_.Count) }

Write-Host ""
Write-Host "Summary by business code:" -ForegroundColor Cyan
$sortedResults |
    Group-Object { if ($null -ne $_.BizCode) { $_.BizCode } else { "null" } } |
    Sort-Object Name |
    ForEach-Object { Write-Host ("{0} -> {1}" -f $_.Name, $_.Count) }

Write-Host ""
Write-Host "Summary by app instance:" -ForegroundColor Cyan
$sortedResults |
    Group-Object { if ([string]::IsNullOrWhiteSpace($_.Instance)) { "none" } else { $_.Instance } } |
    Sort-Object Name |
    ForEach-Object { Write-Host ("{0} -> {1}" -f $_.Name, $_.Count) }

Write-Host ""
Write-Host "Summary by message:" -ForegroundColor Cyan
$sortedResults |
    Group-Object { if ([string]::IsNullOrWhiteSpace($_.Message)) { "<empty>" } else { $_.Message } } |
    Sort-Object Name |
    ForEach-Object { Write-Host ("{0} -> {1}" -f $_.Name, $_.Count) }

$successCount = ($sortedResults | Where-Object { $_.BizCode -eq 200 }).Count
$errorCount = ($sortedResults | Where-Object { $_.BizCode -ne 200 }).Count

Write-Host ""
Write-Host "Final summary:" -ForegroundColor Yellow
Write-Host ("- scheduleId: {0}" -f $ScheduleId)
Write-Host ("- requestCount: {0}" -f $sortedResults.Count)
Write-Host ("- distinctUsers: {0}" -f $sessions.Count)
Write-Host ("- successBiz200: {0}" -f $successCount)
Write-Host ("- nonSuccessBiz: {0}" -f $errorCount)

if ($sessions.Count -eq 1) {
    Write-Host "- expectation: single-user mode usually yields 1 successful enroll and the rest blocked by idempotency and/or rate limiting" -ForegroundColor DarkYellow
} else {
    Write-Host "- expectation: multi-user mode should cap successful enrollments at course capacity" -ForegroundColor DarkYellow
}
