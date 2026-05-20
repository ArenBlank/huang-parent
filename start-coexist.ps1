# huang-parent 共存启动脚本 — 与 VelocityMall 不冲突的端口
$root = "D:\DevelopmentLOOK\Idea\idea_project_workspace\huang-parent"
Set-Location $root

# === 端口偏移配置 ===
$ADMIN_PORT = "8092"
$APP_PORT_1  = "8093"
$APP_PORT_2  = "8094"
$DB_URL      = "jdbc:mysql://127.0.0.1:3307/fitness_platform?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2b8"
$REDIS_PORT  = "6380"
$MINIO_ENDPOINT = "http://127.0.0.1:9010"
$MINIO_PUBLIC_ENDPOINT = "http://localhost:81/minio"

Write-Host "=== huang-parent 共存模式 ===" -ForegroundColor Green
Write-Host "  ADMIN: $ADMIN_PORT  APP1: $APP_PORT_1  APP2: $APP_PORT_2" -ForegroundColor Cyan
Write-Host "  MySQL: 3307  Redis: 6380  MinIO: 9010  Nginx: 81" -ForegroundColor Cyan
Write-Host ""

# [1] Docker 中间件
Write-Host "[1/3] Starting Docker containers..." -ForegroundColor Cyan
docker compose -f docker/docker-compose.yml up -d
Write-Host ""

# [2] 编译
Write-Host "[2/3] Building..." -ForegroundColor Cyan
mvn -pl common,model -am -DskipTests install -q
mvn -pl web/web-admin,web/web-app -am -DskipTests compile -q
Write-Host ""

# [3] 启动 Java 服务
Write-Host "[3/3] Starting Java services..." -ForegroundColor Cyan

$procAdmin = Start-Process powershell -ArgumentList "-NoExit", "-Command",
  "`$env:DB_URL='$DB_URL'; `$env:REDIS_PORT='$REDIS_PORT'; `$env:MINIO_ENDPOINT='$MINIO_ENDPOINT'; `$env:MINIO_PUBLIC_ENDPOINT='$MINIO_PUBLIC_ENDPOINT'; `$env:ADMIN_PORT='$ADMIN_PORT'; Set-Location '$root'; mvn -f web/web-admin/pom.xml -DskipTests spring-boot:run" -PassThru
Write-Host "  web-admin :$ADMIN_PORT (pid=$($procAdmin.Id))"

$procApp1 = Start-Process powershell -ArgumentList "-NoExit", "-Command",
  "`$env:DB_URL='$DB_URL'; `$env:REDIS_PORT='$REDIS_PORT'; `$env:MINIO_ENDPOINT='$MINIO_ENDPOINT'; `$env:MINIO_PUBLIC_ENDPOINT='$MINIO_PUBLIC_ENDPOINT'; `$env:APP_PORT='$APP_PORT_1'; Set-Location '$root'; mvn -f web/web-app/pom.xml -DskipTests spring-boot:run" -PassThru
Write-Host "  web-app-1 :$APP_PORT_1 (pid=$($procApp1.Id))"

$procApp2 = Start-Process powershell -ArgumentList "-NoExit", "-Command",
  "`$env:DB_URL='$DB_URL'; `$env:REDIS_PORT='$REDIS_PORT'; `$env:MINIO_ENDPOINT='$MINIO_ENDPOINT'; `$env:MINIO_PUBLIC_ENDPOINT='$MINIO_PUBLIC_ENDPOINT'; `$env:APP_PORT='$APP_PORT_2'; Set-Location '$root'; mvn -f web/web-app/pom.xml -DskipTests spring-boot:run" -PassThru
Write-Host "  web-app-2 :$APP_PORT_2 (pid=$($procApp2.Id))"

Write-Host ""
Write-Host "=== 访问地址 ===" -ForegroundColor Green
Write-Host "  Nginx 入口: http://localhost:81"
Write-Host "  Admin 后端: http://localhost:$ADMIN_PORT"
Write-Host "  App 后端1:  http://localhost:$APP_PORT_1"
Write-Host "  App 后端2:  http://localhost:$APP_PORT_2"
Write-Host ""
Write-Host "Cpolar 穿透: localhost:81 → huang-parent公网入口"
