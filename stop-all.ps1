$ErrorActionPreference = "Continue"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

function Stop-IfExists {
    param(
        [int]$ProcessId,
        [string]$Label
    )
    if ($ProcessId) {
        Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped $Label pid=$ProcessId"
    }
}

$pidFile = Join-Path $root ".run\pids.json"
if (Test-Path $pidFile) {
    $pids = Get-Content -Raw $pidFile | ConvertFrom-Json
    Stop-IfExists -ProcessId $pids.admin_java_pid -Label "web-admin java"
    Stop-IfExists -ProcessId $pids.app1_java_pid -Label "web-app-1 java"
    Stop-IfExists -ProcessId $pids.app2_java_pid -Label "web-app-2 java"
    Stop-IfExists -ProcessId $pids.admin_wrapper_pid -Label "web-admin wrapper"
    Stop-IfExists -ProcessId $pids.app1_wrapper_pid -Label "web-app-1 wrapper"
    Stop-IfExists -ProcessId $pids.app2_wrapper_pid -Label "web-app-2 wrapper"
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
} else {
    Write-Host "No pid file found, trying port-based stop..."
}

Get-NetTCPConnection -LocalPort 8080,8081,8082 -State Listen -ErrorAction SilentlyContinue |
Select-Object -ExpandProperty OwningProcess -Unique |
ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }

# Optional: stop external containers
Push-Location (Join-Path $root "docker")
docker compose stop | Out-Null
Pop-Location
Write-Host "Containers stopped."
