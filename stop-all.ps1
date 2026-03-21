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
    Stop-IfExists -ProcessId $pids.app_java_pid -Label "web-app java"
    Stop-IfExists -ProcessId $pids.admin_wrapper_pid -Label "web-admin wrapper"
    Stop-IfExists -ProcessId $pids.app_wrapper_pid -Label "web-app wrapper"
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
} else {
    Write-Host "No pid file found, trying port-based stop..."
}

Get-NetTCPConnection -LocalPort 8080,8081 -State Listen -ErrorAction SilentlyContinue |
Select-Object -ExpandProperty OwningProcess -Unique |
ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }

# Optional: stop external containers
docker stop mysql-container-huang redis-container-huang minio-container-huang nginx-container-huang rabbitmq-container-huang | Out-Null
Write-Host "Containers stopped."
