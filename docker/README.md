# Docker 目录说明

这个目录现在的定位是：

- 负责中间件与 Nginx 反向代理
- 适配“本地联调”和“个人服务器面试演示”
- 不把 Spring Boot 后端和 Vue 前端强行全部容器化

这样做的好处是：

- 和当前项目结构最接近
- 改动小
- 出问题更容易排查
- 适合你现在的目标：先稳定上到自己的服务器，再给面试官演示

## 目录结构

```text
docker/
  .env.example
  docker-compose.yml
  README.md
  env/
    admin.env.example
    app.env.example
  mysql/
    init/
      001-create-database.sql
    data/
  redis/
    data/
  minio/
    data/
  nginx/
    templates/
      fitness.conf.template
```

## 这套目录解决了什么问题

相比之前“只有空目录”的状态，现在这里已经补齐了：

- 可直接启动的 `docker-compose.yml`
- 可复用的 `.env` 样例
- MySQL 初始化脚本
- Nginx 模板
- 后端 `prod` 环境变量样例
- 数据目录忽略规则

## 使用方式

### 1. 准备 compose 环境变量

在 `D:\DevelopmentLOOK\Idea\idea_project_workspace\huang-parent\docker` 下复制：

```powershell
Copy-Item .env.example .env
```

本地联调时，默认值就能用：

- App 前端域名：`app.localhost`
- Admin 前端域名：`admin.localhost`
- 文件域名：`files.localhost`

如果是服务器部署，把它们改成你自己的真实域名，例如：

- `app.xxx.com`
- `admin.xxx.com`
- `files.xxx.com`

### 2. 构建前端静态文件

在项目根目录执行：

```powershell
cd D:\DevelopmentLOOK\Idea\idea_project_workspace\huang-parent\frontend
npm run build

cd D:\DevelopmentLOOK\Idea\idea_project_workspace\huang-parent\frontend-app
npm run build
```

生成的 `dist` 会被 Nginx 容器直接挂载。

### 3. 启动 Docker 中间件和 Nginx

```powershell
cd D:\DevelopmentLOOK\Idea\idea_project_workspace\huang-parent\docker
docker compose up -d
```

### 4. 导入数据库结构和测试数据

首次启动只会自动创建数据库，不会自动导入完整业务表。

导入方式：

```powershell
Get-Content ..\\fitness_platform.sql | docker exec -i mysql-container-huang mysql -uroot -proot fitness_platform
```

### 5. 启动两个后端

你现在最适合的方式仍然是：

- `web-admin` 跑在宿主机 `8080`
- `web-app` 跑在宿主机 `8081`

Nginx 容器已经通过 `host.docker.internal` 反代到这两个端口。

本地可直接用：

```powershell
cd D:\DevelopmentLOOK\Idea\idea_project_workspace\huang-parent
powershell -ExecutionPolicy Bypass -File start-all.ps1
```

服务器上更推荐：

- 打包成 jar
- 用 `systemd` 托管

## 访问方式

本地联调建议使用：

- [http://app.localhost](http://app.localhost)
- [http://admin.localhost](http://admin.localhost)
- [http://files.localhost](http://files.localhost)

说明：

- `app.localhost`：用户端前端
- `admin.localhost`：管理端前端
- `files.localhost`：MinIO 文件代理域名

`files.localhost` 这一层很重要，因为视频和头像的签名 URL 需要一个浏览器可访问的公共域名入口，不能直接写服务器内网地址。

当前模板默认通过 `host.docker.internal:9000` 反代宿主机上的 MinIO 端口。
这样即使你暂时没有把 MySQL / Redis / MinIO 全部纳入 `docker compose` 管理，Nginx 这层也能先稳定跑起来。

## 对应的后端环境变量

如果后端运行在宿主机，建议用下面两个样例：

- `D:\DevelopmentLOOK\Idea\idea_project_workspace\huang-parent\docker\env\admin.env.example`
- `D:\DevelopmentLOOK\Idea\idea_project_workspace\huang-parent\docker\env\app.env.example`

其中最关键的一项是：

- `MINIO_ENDPOINT`

本地建议：

- `MINIO_ENDPOINT=http://files.localhost`

服务器建议：

- `MINIO_ENDPOINT=https://files.xxx.com`

## 这套方案的边界

这次只把 `docker` 目录优化成“可部署的基础骨架”，边界明确如下：

- 包含：MySQL、Redis、MinIO、Nginx
- 包含：前端静态资源托管
- 包含：反代宿主机后端
- 不包含：Spring Boot 后端容器化
- 不包含：HTTPS 证书自动签发
- 不包含：完整生产级监控
- 不包含：Kubernetes

也就是说，它已经足够支撑你的“个人服务器面试演示版”，但不是要一步到位做成复杂生产集群。
