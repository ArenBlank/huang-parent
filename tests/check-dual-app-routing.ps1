param(
    [string]$Url = "http://localhost/app/banner/list",
    [string]$HostHeader = "app.localhost",
    [int]$Requests = 12,
    [int]$RetryCount = 20,
    [int]$RetryDelayMs = 1000,
    [string]$Token,
    [hashtable]$ExtraHeaders,
    [int]$MinDistinctInstances = 0
)

$ErrorActionPreference = "Stop"

$instanceStats = @{}
$headers = @{}
if (-not [string]::IsNullOrWhiteSpace($HostHeader)) {
    $headers["Host"] = $HostHeader
}
if (-not [string]::IsNullOrWhiteSpace($Token)) {
    $headers["Authorization"] = "Bearer $Token"
}
if ($ExtraHeaders) {
    foreach ($key in $ExtraHeaders.Keys) {
        $headers[$key] = $ExtraHeaders[$key]
    }
}

function Invoke-WithRetry {
    param(
        [scriptblock]$Action,
        [int]$MaxAttempts,
        [int]$DelayMs
    )

    $attempt = 0
    do {
        $attempt++
        try {
            return & $Action
        } catch {
            if ($attempt -ge $MaxAttempts) {
                throw
            }
            Start-Sleep -Milliseconds $DelayMs
        }
    } while ($true)
}

for ($i = 1; $i -le $Requests; $i++) {
    $response = Invoke-WithRetry -MaxAttempts $RetryCount -DelayMs $RetryDelayMs -Action {
        Invoke-WebRequest -Uri $Url -Method Get -Headers $headers -UseBasicParsing
    }
    $instanceHeader = $response.Headers["X-App-Instance"]
    $instance = @($instanceHeader | Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_) }) | Select-Object -First 1
    if ([string]::IsNullOrWhiteSpace($instance)) {
        $instance = "missing-header"
    }
    if (-not $instanceStats.ContainsKey($instance)) {
        $instanceStats[$instance] = 0
    }
    $instanceStats[$instance] = [int]$instanceStats[$instance] + 1
    Write-Host ("[{0}/{1}] status={2} instance={3}" -f $i, $Requests, $response.StatusCode, $instance)
}

Write-Host ""
Write-Host "Instance distribution:" -ForegroundColor Cyan
$instanceStats.GetEnumerator() |
    Sort-Object Name |
    ForEach-Object { Write-Host ("{0} -> {1}" -f $_.Name, $_.Value) }

if ($MinDistinctInstances -gt 0 -and $instanceStats.Count -lt $MinDistinctInstances) {
    throw ("Only {0} distinct instance(s) observed, expected at least {1}" -f $instanceStats.Count, $MinDistinctInstances)
}
