# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

智训健身平台，以前后端联动为目标、以后端治理能力为核心的工程型单体项目。后端 Spring Boot 3 + MyBatis-Plus + MySQL + Redis + Caffeine + MinIO，前端 Vue 3 + Vite + Element Plus + uni-app。

## 构建与运行

### 后端（Maven，Java 17）

```bash
# 编译 + 打包（跳过测试）
mvn -B -ntp -pl web/web-admin,web/web-app -am -DskipTests package

# 安装 common/model 到本地仓库
mvn -B -ntp -pl common,model -am -DskipTests install

# 启动 web-admin（端口 8092）
mvn -f web/web-admin/pom.xml -DskipTests spring-boot:run

# 启动 web-app 实例一（端口 8093）
mvn -f web/web-app/pom.xml -DskipTests spring-boot:run

# 启动 web-app 实例二（端口 8094）
mvn -f web/web-app/pom.xml -DskipTests spring-boot:run -Dspring-boot.run.arguments="--server.port=8094"

# 指定 profile
mvn -f web/web-admin/pom.xml -DskipTests spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
```

Profile 选项：`dev`（默认）、`test`、`prod`。配置文件在 `web/web-{admin,app}/src/main/resources/application-{profile}.yml`。

### 前端

```bash
# 管理端（端口 5173）
cd frontend && npm run dev       # 开发
cd frontend && npm run build     # 构建

# 用户端 Web（端口 5174）
cd frontend-app && npm run dev
cd frontend-app && npm run build

# uni-app 用户端 H5（端口 5175）
cd frontend-uniapp && npm run dev:h5
cd frontend-uniapp && npm run build:h5
```

### Docker 中间件

```bash
cd docker
docker compose --env-file .env up -d    # 启动 MySQL/Redis/MinIO/Nginx
docker compose down                     # 停止
```

### 一键脚本（PowerShell）

```powershell
powershell -ExecutionPolicy Bypass -File start-all.ps1            # 一键启动所有服务
powershell -ExecutionPolicy Bypass -File stop-all.ps1             # 停止
powershell -ExecutionPolicy Bypass -File tests/reset-test-data.ps1 # 重置测试数据
```

### 测试

```bash
# Newman 回归测试
newman run tests/fitness-platform-core.postman_collection.json -e tests/local.postman_environment.json

# 双实例轮询验证
powershell -ExecutionPolicy Bypass -File tests/check-dual-app-routing.ps1

# 并发验证
powershell -ExecutionPolicy Bypass -File tests/invoke-concurrent-requests.ps1
```

## 模块架构

```
huang-parent/
├── common/          # 通用组件：JWT、Redis、缓存、守卫（限流/幂等/分布式锁）、异常、结果封装、AI 配置
├── model/           # 实体 Entity 类 + 基础枚举（项目唯一实体模块，其他模块不得创建 Entity）
├── web/
│   ├── web-admin/   # 管理端：完整 MVC（controller → service/biz → service/impl → mapper）+ 独立 Knife4j
│   └── web-app/     # 用户端：完整 MVC（同上）+ 独立 Knife4j
├── frontend/        # 管理端前端（Vue 3 + Vite + Element Plus，端口 5173）
├── frontend-app/    # 用户端 Web 前端（Vue 3 + Vite + Element Plus，端口 5174）
├── frontend-uniapp/ # 用户端 uni-app（H5 + 微信小程序，端口 5175）
├── tests/           # Postman 集合 + PowerShell 验证脚本
├── docker/          # docker-compose.yml + Nginx 模板
├── deploy/          # Nginx 配置
└── doc/             # 设计文档
```

## 关键架构约束

### 双端完全独立

`web-admin` 和 `web-app` 各自拥有完整的 controller、service、mapper、config 层，互不共享。新增功能时需明确归属到 admin 还是 app，然后在该模块内完成全栈分层。Entity 只放在 `model` 模块，通用工具/组件放在 `common` 模块。

### 服务分层（务必遵守）

```
controller        → 参数校验、鉴权入口、Result 封装，不写跨表业务，不直接调用 mapper
service/biz       → 业务编排层，状态机、事务边界、跨表流程（唯一允许跨表写操作的地方）
service + impl    → 原子能力薄层，单表 CRUD 与简单查询
mapper            → 数据访问层，不写业务判断
```

- 状态流转只允许在 `biz` 层触发
- 跨表写操作必须在 `biz` 层开启事务
- controller 禁止直接调用 mapper
- 登录态由 JWT + `LoginUserHolder` 提供，业务代码用 `LoginUserHolder.getLoginUser()` 获取，不传 `userId` 参数

### 缓存策略

- 只对静态低频数据使用 `Caffeine 本地 + Redis 共享` 两级缓存：Banner、Notice、SystemConfig、Course List、Plan List、Plan Detail Static
- 动态交易数据绝不进入本地缓存：订单、支付状态、退款状态、排期余量、预约状态
- 近实时失效流程：事务提交 → 删 Redis 共享缓存 → 删本机 Caffeine → 发布 `cache:evict` Pub/Sub → 其他实例删自己的本地缓存
- `MultiLevelCacheSupport` 是缓存操作的统一入口，不要直接操作 LocalCacheSupport 或 RedisCacheSupport

### 并发治理三层体系

1. 应用层：`@RateLimit`（固定窗口限流）、`@IdempotentSubmit`（短生命周期幂等键），通过 `RequestGuardAspect` AOP 切入所有 controller 方法
2. Redis 层：`RedisGuardSupport`（限流/幂等判决）、`RedisLockLuaSupport`（SET NX EX 加锁 + Lua compare-and-delete 释放）
3. 数据库层：事务 + 条件更新 + 唯一索引兜底最终正确性

### 鉴权

- 无状态 JWT，token 携 `platform + type + tokenVersion`
- Admin 鉴权：`JwtUtil` 解析 + 管理员角色注释 `@RequireAdminRole`
- App 鉴权：`AuthenticationInterceptor` 拦截 + `AppAuthCacheService` 做 Redis L2 缓存 + `tokenVersion` 校验

### 支付验签模式

`payment.sign.mode`：`mock`（本地演示）或 `hmac`（CI/生产）

## 常用本地地址

| 服务 | 地址 |
|------|------|
| 管理端后端 | `http://localhost:8092` |
| 用户端后端实例一 | `http://localhost:8093` |
| 用户端后端实例二 | `http://localhost:8094` |
| 管理端前端 | `http://localhost:5173` |
| 用户端前端 | `http://localhost:5174` |
| uni-app H5 | `http://localhost:5175` |
| Nginx 代理 | `http://localhost:81` |
| Knife4j Admin | `http://localhost:8092/doc.html` |
| Knife4j App | `http://localhost:8093/doc.html` |
| MinIO Console | `http://localhost:9011` |
