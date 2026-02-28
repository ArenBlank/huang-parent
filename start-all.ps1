$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$runDir = Join-Path $root ".run"
if (-not (Test-Path $runDir)) {
    New-Item -ItemType Directory -Path $runDir | Out-Null
}

Write-Host "[1/4] Starting external containers..." -ForegroundColor Cyan
docker start mysql-container-huang redis-container-huang minio-container-huang nginx-container-huang rabbitmq-container-huang | Out-Null

Write-Host "[2/4] Building shared modules..." -ForegroundColor Cyan
mvn -pl common,model -am -DskipTests install
mvn -pl web/web-admin,web/web-app -am -DskipTests compile

Write-Host "[3/4] Starting web-admin (8080)..." -ForegroundColor Cyan
$adminProc = Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$root'; mvn -f web/web-admin/pom.xml -DskipTests spring-boot:run" -PassThru

Write-Host "[4/4] Starting web-app (8081)..." -ForegroundColor Cyan
$appProc = Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$root'; mvn -f web/web-app/pom.xml -DskipTests spring-boot:run" -PassThru

$pidFile = Join-Path $runDir "pids.json"
@{
    admin_pid = $adminProc.Id
    app_pid = $appProc.Id
    started_at = (Get-Date).ToString("s")
} | ConvertTo-Json | Set-Content -Encoding UTF8 $pidFile

Write-Host "Done." -ForegroundColor Green
Write-Host "web-admin: http://localhost:8080"
Write-Host "web-app:   http://localhost:8081"
