$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$runDir = Join-Path $root ".run"
if (-not (Test-Path $runDir)) {
    New-Item -ItemType Directory -Path $runDir | Out-Null
}

function Stop-ListeningProcess {
    param(
        [int]$Port
    )
    $listeners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($pid in $listeners) {
        if ($pid) {
            Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
            Write-Host "Stopped stale process on port $Port pid=$pid" -ForegroundColor Yellow
        }
    }
}

function Wait-PortReady {
    param(
        [int]$Port,
        [string]$Label,
        [int]$TimeoutSeconds = 60
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $listener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($listener) {
            Write-Host "$Label is listening on $Port pid=$($listener.OwningProcess)" -ForegroundColor Green
            return $listener.OwningProcess
        }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $deadline)

    throw "$Label did not start listening on port $Port within $TimeoutSeconds seconds"
}

Write-Host "[0/4] Cleaning stale local listeners..." -ForegroundColor Cyan
Stop-ListeningProcess -Port 8080
Stop-ListeningProcess -Port 8081

Write-Host "[1/4] Starting external containers..." -ForegroundColor Cyan
docker start mysql-container-huang redis-container-huang minio-container-huang nginx-container-huang rabbitmq-container-huang | Out-Null

Write-Host "[2/4] Building shared modules..." -ForegroundColor Cyan
mvn -pl common,model -am -DskipTests install
mvn -pl web/web-admin,web/web-app -am -DskipTests compile

Write-Host "[3/4] Starting web-admin (8080)..." -ForegroundColor Cyan
$adminProc = Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$root'; mvn -f web/web-admin/pom.xml -DskipTests spring-boot:run" -PassThru

Write-Host "[4/4] Starting web-app (8081)..." -ForegroundColor Cyan
$appProc = Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$root'; mvn -f web/web-app/pom.xml -DskipTests spring-boot:run" -PassThru

$adminJavaPid = Wait-PortReady -Port 8080 -Label "web-admin"
$appJavaPid = Wait-PortReady -Port 8081 -Label "web-app"

$pidFile = Join-Path $runDir "pids.json"
@{
    admin_wrapper_pid = $adminProc.Id
    app_wrapper_pid = $appProc.Id
    admin_java_pid = $adminJavaPid
    app_java_pid = $appJavaPid
    started_at = (Get-Date).ToString("s")
} | ConvertTo-Json | Set-Content -Encoding UTF8 $pidFile

Write-Host "Done." -ForegroundColor Green
Write-Host "web-admin: http://localhost:8080"
Write-Host "web-app:   http://localhost:8081"
