param(
    [Parameter(Mandatory = $true)]
    [string]$Url,

    [ValidateSet("GET", "POST")]
    [string]$Method = "POST",

    [string]$Body,
    [string]$Token,
    [int]$Concurrency = 10,
    [string]$HostHeader,
    [int]$RetryCount = 5,
    [int]$RetryDelayMs = 500,
    [hashtable]$ExtraHeaders
)

$ErrorActionPreference = "Stop"

$headers = @{
    "Content-Type" = "application/json"
}
if ($Token) {
    $headers["Authorization"] = "Bearer $Token"
}
if ($HostHeader) {
    $headers["Host"] = $HostHeader
}
if ($ExtraHeaders) {
    foreach ($key in $ExtraHeaders.Keys) {
        $headers[$key] = $ExtraHeaders[$key]
    }
}

$jobs = @()
for ($i = 1; $i -le $Concurrency; $i++) {
    $jobs += Start-Job -ScriptBlock {
        param($Url, $Method, $Body, $Headers, $Index, $RetryCount, $RetryDelayMs)
        try {
            $response = $null
            $lastError = $null
            for ($attempt = 1; $attempt -le $RetryCount; $attempt++) {
                try {
                    if ($Method -eq "GET") {
                        $response = Invoke-WebRequest -Uri $Url -Method Get -Headers $Headers -UseBasicParsing
                    } else {
                        $response = Invoke-WebRequest -Uri $Url -Method Post -Headers $Headers -Body $Body -UseBasicParsing
                    }
                    break
                } catch {
                    $lastError = $_
                    if ($attempt -lt $RetryCount) {
                        Start-Sleep -Milliseconds $RetryDelayMs
                    }
                }
            }
            if ($null -eq $response) {
                throw $lastError
            }
            $payload = $null
            if ($response.Content) {
                try {
                    $payload = $response.Content | ConvertFrom-Json
                } catch {
                    $payload = $response.Content
                }
            }
            [pscustomobject]@{
                Index = $Index
                HttpStatus = $response.StatusCode
                Instance = $response.Headers["X-App-Instance"]
                Body = $payload
                Error = $null
            }
        } catch {
            [pscustomobject]@{
                Index = $Index
                HttpStatus = -1
                Instance = $null
                Body = $null
                Error = $_.Exception.Message
            }
        }
    } -ArgumentList $Url, $Method, $Body, $headers, $i, $RetryCount, $RetryDelayMs
}

$results = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job -Force | Out-Null

Write-Host "Per request result:" -ForegroundColor Cyan
$results |
    Sort-Object Index |
    ForEach-Object {
        $message = $_.Error
        if (-not $message -and $_.Body -is [pscustomobject]) {
            $message = $_.Body.message
        }
        Write-Host ("#{0} status={1} instance={2} message={3}" -f $_.Index, $_.HttpStatus, $_.Instance, $message)
    }

Write-Host ""
Write-Host "Summary by http status:" -ForegroundColor Cyan
$results |
    Group-Object HttpStatus |
    Sort-Object Name |
    ForEach-Object { Write-Host ("{0} -> {1}" -f $_.Name, $_.Count) }

Write-Host ""
Write-Host "Summary by app instance:" -ForegroundColor Cyan
$results |
    Group-Object { if ($_.Instance) { $_.Instance } else { "none" } } |
    Sort-Object Name |
    ForEach-Object { Write-Host ("{0} -> {1}" -f $_.Name, $_.Count) }
