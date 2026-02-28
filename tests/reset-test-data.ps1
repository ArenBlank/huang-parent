param(
  [string]$ContainerName = "mysql-container-huang",
  [string]$DbName = "fitness_platform",
  [string]$User = "root",
  [string]$Password = "root"
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$sqlPath = Join-Path $scriptDir "reset_test_data.sql"

if (-not (Test-Path $sqlPath)) {
  Write-Error "SQL file not found: $sqlPath"
  exit 1
}

Write-Host "Applying reset script to container '$ContainerName' db '$DbName'..."
$sql = Get-Content $sqlPath -Raw
$sql | docker exec -i $ContainerName mysql "--user=$User" "--password=$Password" $DbName

if ($LASTEXITCODE -ne 0) {
  Write-Error "Reset failed. Check container name / mysql credentials."
  exit $LASTEXITCODE
}

Write-Host "Reset completed."
