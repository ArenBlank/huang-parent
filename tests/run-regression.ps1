param(
    [ValidateSet("core", "full")]
    [string]$Suite = "full",
    [switch]$ResetData,
    [string]$EnvironmentFile = "tests/local.postman_environment.json",
    [string]$VideoFilePath = "tests/assets/demo.mp4",
    [ValidateSet("mock", "hmac")]
    [string]$PaymentSignMode = "mock",
    [string]$PaymentSignSecret = "ci-pay-secret"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path | Split-Path -Parent
Set-Location $root

if (-not (Test-Path $EnvironmentFile)) {
    throw "Environment file not found: $EnvironmentFile"
}

if (-not (Test-Path $VideoFilePath)) {
    throw "Video file not found: $VideoFilePath"
}

$collectionFile = if ($Suite -eq "core") {
    "tests/fitness-platform-core.postman_collection.json"
} else {
    "tests/fitness-platform-full.postman_collection.json"
}

if (-not (Test-Path $collectionFile)) {
    throw "Collection file not found: $collectionFile"
}

if ($ResetData) {
    Write-Host "[1/3] Resetting test data..." -ForegroundColor Cyan
    powershell -ExecutionPolicy Bypass -File tests/reset-test-data.ps1
} else {
    Write-Host "[1/3] Skip reset test data." -ForegroundColor DarkYellow
}

$newman = Get-Command newman -ErrorAction SilentlyContinue
if (-not $newman) {
    Write-Host "[2/3] newman not found, installing globally..." -ForegroundColor Yellow
    npm install -g newman | Out-Host
    $newman = Get-Command newman -ErrorAction Stop
} else {
    Write-Host "[2/3] Using newman at $($newman.Source)" -ForegroundColor Cyan
}

$args = @(
    "run", $collectionFile,
    "-e", $EnvironmentFile,
    "--working-dir", $root,
    "--env-var", "videoFilePath=$VideoFilePath",
    "--env-var", "paymentSignMode=$PaymentSignMode",
    "--env-var", "paymentSignSecret=$PaymentSignSecret",
    "--reporters", "cli"
)

Write-Host "[3/3] Running suite=$Suite paymentSignMode=$PaymentSignMode" -ForegroundColor Green
& $newman.Source @args
