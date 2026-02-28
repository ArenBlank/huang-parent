$ErrorActionPreference = "Continue"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$pidFile = Join-Path $root ".run\pids.json"
if (Test-Path $pidFile) {
    $pids = Get-Content -Raw $pidFile | ConvertFrom-Json
    if ($pids.admin_pid) {
        Stop-Process -Id $pids.admin_pid -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped web-admin pid=$($pids.admin_pid)"
    }
    if ($pids.app_pid) {
        Stop-Process -Id $pids.app_pid -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped web-app pid=$($pids.app_pid)"
    }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
} else {
    Write-Host "No pid file found, trying port-based stop..."
    Get-NetTCPConnection -LocalPort 8080,8081 -State Listen -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique |
    ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }
}

# Optional: stop external containers
docker stop mysql-container-huang redis-container-huang minio-container-huang nginx-container-huang rabbitmq-container-huang | Out-Null
Write-Host "Containers stopped."
