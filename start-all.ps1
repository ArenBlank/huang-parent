param(
    [switch]$UseExternalInfra
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$runDir = Join-Path $root ".run"
if (-not (Test-Path $runDir)) {
    New-Item -ItemType Directory -Path $runDir | Out-Null
}

Write-Host "[1/5] Starting containers..." -ForegroundColor Cyan
if ($UseExternalInfra) {
    Write-Host "Using external MySQL/Redis/MinIO, skip docker compose startup." -ForegroundColor Yellow
} else {
    Write-Host "Using project local infra (MySQL/Redis/MinIO)." -ForegroundColor Yellow
    docker compose --profile infra-local up -d
}

Write-Host "[2/5] Cleaning/building and installing shared modules..." -ForegroundColor Cyan
mvn -pl common,model -am -DskipTests clean install
mvn -pl web/web-admin,web/web-app -am -DskipTests compile

Write-Host "[3/5] Starting web-admin (8080)..." -ForegroundColor Cyan
$adminProc = Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$root'; mvn -f web/web-admin/pom.xml -DskipTests spring-boot:run" -PassThru

Write-Host "[4/5] Starting web-app (8081)..." -ForegroundColor Cyan
$appProc = Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$root'; mvn -f web/web-app/pom.xml -DskipTests spring-boot:run" -PassThru

$pidFile = Join-Path $runDir "pids.json"
@{
    admin_pid = $adminProc.Id
    app_pid   = $appProc.Id
    started_at = (Get-Date).ToString("s")
    use_external_infra = [bool]$UseExternalInfra
} | ConvertTo-Json | Set-Content -Encoding UTF8 $pidFile

Write-Host "[5/5] Done." -ForegroundColor Green
Write-Host "web-admin: http://localhost:8080"
Write-Host "web-app:   http://localhost:8081"
Write-Host "MinIO:     http://localhost:9001"
Write-Host ""
Write-Host "Stop all with: .\stop-all.ps1"
