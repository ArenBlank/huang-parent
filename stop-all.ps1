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
    Write-Host "No pid file found, skipping app process stop."
}

docker compose down
Write-Host "Compose services stopped."
