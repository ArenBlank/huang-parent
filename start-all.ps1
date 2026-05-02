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
    foreach ($processId in $listeners) {
        if ($processId) {
            Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
            Write-Host "Stopped stale process on port $Port pid=$processId" -ForegroundColor Yellow
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

function Ensure-MinioBucket {
    param(
        [string]$MinioRootUser,
        [string]$MinioRootPassword,
        [string]$BucketName
    )

    if (-not $MinioRootUser) {
        $MinioRootUser = "root"
    }
    if (-not $MinioRootPassword) {
        $MinioRootPassword = "rootroot"
    }
    if (-not $BucketName) {
        $BucketName = "lease"
    }

    Write-Host "[1.5/4] Ensuring MinIO bucket '$BucketName'..." -ForegroundColor Cyan
    docker run --rm --entrypoint /bin/sh --network "container:minio-container-huang" minio/mc:latest `
        -c "until mc alias set local http://127.0.0.1:9000 $MinioRootUser $MinioRootPassword; do echo waiting-for-minio; sleep 2; done; mc mb --ignore-existing local/$BucketName"

    if ($LASTEXITCODE -ne 0) {
        throw "Failed to initialize MinIO bucket '$BucketName'"
    }
}

function Invoke-DockerCompose {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    Push-Location (Join-Path $root "docker")
    try {
        & docker compose @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "docker compose $($Arguments -join ' ') failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
}

function Get-ContainerRunningState {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ContainerName
    )

    $state = docker inspect --format "{{.State.Running}}" $ContainerName 2>$null
    if ($LASTEXITCODE -ne 0) {
        return $null
    }
    return ($state | Out-String).Trim()
}

function Wait-ContainerRunning {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ContainerName,
        [int]$TimeoutSeconds = 40
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        if ((Get-ContainerRunningState -ContainerName $ContainerName) -eq "true") {
            Write-Host "$ContainerName is running" -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $deadline)

    throw "$ContainerName did not reach running state within $TimeoutSeconds seconds"
}

function Ensure-CoreContainersRunning {
    $containerNames = @(
        "mysql-container-huang",
        "redis-container-huang",
        "minio-container-huang",
        "nginx-container-huang"
    )

    foreach ($containerName in $containerNames) {
        if ((Get-ContainerRunningState -ContainerName $containerName) -ne "true") {
            Write-Host "[1.2/4] $containerName was not running after compose up, retrying targeted start..." -ForegroundColor Yellow
            switch ($containerName) {
                "mysql-container-huang" { Invoke-DockerCompose -Arguments @("up", "-d", "mysql") }
                "redis-container-huang" { Invoke-DockerCompose -Arguments @("up", "-d", "redis") }
                "minio-container-huang" { Invoke-DockerCompose -Arguments @("up", "-d", "minio") }
                "nginx-container-huang" { Invoke-DockerCompose -Arguments @("up", "-d", "nginx") }
            }
        }
        Wait-ContainerRunning -ContainerName $containerName
    }
}

function Resolve-EnvValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [string]$DefaultValue = ""
    )

    $current = [Environment]::GetEnvironmentVariable($Name, "Process")
    if ($current) {
        return $current
    }

    $userValue = [Environment]::GetEnvironmentVariable($Name, "User")
    if ($userValue) {
        [Environment]::SetEnvironmentVariable($Name, $userValue, "Process")
        return $userValue
    }

    $machineValue = [Environment]::GetEnvironmentVariable($Name, "Machine")
    if ($machineValue) {
        [Environment]::SetEnvironmentVariable($Name, $machineValue, "Process")
        return $machineValue
    }

    if ($DefaultValue) {
        [Environment]::SetEnvironmentVariable($Name, $DefaultValue, "Process")
        return $DefaultValue
    }

    return ""
}

Write-Host "[0/4] Cleaning stale local listeners..." -ForegroundColor Cyan
Stop-ListeningProcess -Port 8080
Stop-ListeningProcess -Port 8081
Stop-ListeningProcess -Port 8082

$llmApiKey = Resolve-EnvValue -Name "LLM_API_KEY"
$llmBaseUrl = Resolve-EnvValue -Name "LLM_BASE_URL" -DefaultValue "https://api.deepseek.com"
$llmModelName = Resolve-EnvValue -Name "LLM_MODEL_NAME" -DefaultValue "deepseek-chat"
$llmTimeout = Resolve-EnvValue -Name "LLM_TIMEOUT" -DefaultValue "60s"

if (-not $llmApiKey) {
    Write-Host "[WARN] LLM_API_KEY is empty. AI plan generation will use placeholder and fail authentication." -ForegroundColor Yellow
} else {
    Write-Host "[0/4] Loaded LLM env: base=$llmBaseUrl model=$llmModelName timeout=$llmTimeout keyLen=$($llmApiKey.Length)" -ForegroundColor Green
}

Write-Host "[1/4] Starting external containers..." -ForegroundColor Cyan
Invoke-DockerCompose -Arguments @("up", "-d")
Ensure-CoreContainersRunning
Ensure-MinioBucket -MinioRootUser $env:MINIO_ROOT_USER -MinioRootPassword $env:MINIO_ROOT_PASSWORD -BucketName $env:MINIO_BUCKET

Write-Host "[2/4] Building shared modules..." -ForegroundColor Cyan
mvn -pl common,model -am -DskipTests install
mvn -pl web/web-admin,web/web-app -am -DskipTests compile

Write-Host "[3/4] Starting web-admin (8080)..." -ForegroundColor Cyan
$adminProc = Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$env:ADMIN_PORT='8080'; Set-Location '$root'; mvn -f web/web-admin/pom.xml -DskipTests spring-boot:run" -PassThru

Write-Host "[4/4] Starting web-app instance-1 (8081)..." -ForegroundColor Cyan
$appProc1 = Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$env:APP_PORT='8081'; Set-Location '$root'; mvn -f web/web-app/pom.xml -DskipTests spring-boot:run" -PassThru

Write-Host "[4/4] Starting web-app instance-2 (8082)..." -ForegroundColor Cyan
$appProc2 = Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$env:APP_PORT='8082'; Set-Location '$root'; mvn -f web/web-app/pom.xml -DskipTests spring-boot:run" -PassThru

$adminJavaPid = Wait-PortReady -Port 8080 -Label "web-admin"
$appJavaPid1 = Wait-PortReady -Port 8081 -Label "web-app instance-1"
$appJavaPid2 = Wait-PortReady -Port 8082 -Label "web-app instance-2"

$pidFile = Join-Path $runDir "pids.json"
@{
    admin_wrapper_pid = $adminProc.Id
    app1_wrapper_pid = $appProc1.Id
    app2_wrapper_pid = $appProc2.Id
    admin_java_pid = $adminJavaPid
    app1_java_pid = $appJavaPid1
    app2_java_pid = $appJavaPid2
    started_at = (Get-Date).ToString("s")
} | ConvertTo-Json | Set-Content -Encoding UTF8 $pidFile

Write-Host "Done." -ForegroundColor Green
Write-Host "web-admin: http://localhost:8080"
Write-Host "web-app-1: http://localhost:8081"
Write-Host "web-app-2: http://localhost:8082"
