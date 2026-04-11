# 运动健康管理平台后端工程

![CI](https://github.com/ArenBlank/huang-parent/actions/workflows/ci.yml/badge.svg?branch=V3)
![Integration](https://github.com/ArenBlank/huang-parent/actions/workflows/integration.yml/badge.svg?branch=V3)

这是一个基于 `Spring Boot + MySQL + Redis + Nginx + MinIO` 的运动健康管理平台后端工程，采用“`admin` 单实例 + `app` 可双实例”的工程型单体架构。当前版本重点落在多实例部署、Redis 并发治理、多级缓存、防重复提交、支付回调幂等、RBAC 权限和自动化回归验证。

## 项目定位

- 工程型单体项目
- 中小规模高并发治理场景
- 以 `MySQL + Redis + Nginx` 为核心的一致性与性能优化项目

## 当前部署形态

- `web-admin`：单实例，默认端口 `8080`
- `web-app`：支持双实例，默认端口 `8081` / `8082`
- `Nginx`：通过 `upstream` 轮询转发到两个 `web-app` 实例
- 认证方式：`JWT` 无状态认证，不依赖会话粘滞

## 当前技术亮点

- 双实例部署：`Nginx upstream + dual-instance deployment`
- 并发治理：`应用层幂等/限流 + Redis 短锁/短状态 + 数据库条件更新/唯一索引兜底`
- 缓存治理：`Caffeine + Redis` 多级缓存、`Cache-Aside`、`TTL jitter`、空值缓存、热点互斥重建
- 支付回调：短锁 + 状态校验 + 审计日志，避免重复入账
- 可观测性：`traceId + userId + instanceId` 链路日志
- 自动化验证：`Postman/Newman + GitHub Actions + 双实例 smoke test`

## 已完成验证

- Maven 编译与测试通过
- Newman 全量回归通过：`127 requests / 389 assertions / 0 failed`
- Nginx 双实例轮询验证通过
- JWT 跨实例访问验证通过
- Caffeine + Redis + Redis Pub/Sub 近实时失效验证通过

## 快速开始

1. 启动依赖与服务

```powershell
powershell -ExecutionPolicy Bypass -File start-all.ps1
```

2. 重置测试数据

```powershell
powershell -ExecutionPolicy Bypass -File tests/reset-test-data.ps1
```

3. 常用本地地址

- Admin 后端：`http://localhost:8080`
- App 后端实例一：`http://localhost:8081`
- App 后端实例二：`http://localhost:8082`
- Nginx：`http://localhost`

## 文档导航

- 项目总览与亮点：[doc/工程技术亮点汇总.md](./doc/工程技术亮点汇总.md)
- 双实例与 Redis 并发治理：[doc/单体双实例与Redis并发落地说明.md](./doc/单体双实例与Redis并发落地说明.md)
- 简历项目亮点：[doc/简历项目亮点（真实增强版）.md](./doc/简历项目亮点（真实增强版）.md)
- 并发方案对比：[doc/并发方案样板场景对比.md](./doc/并发方案样板场景对比.md)
- 部署与环境配置：[doc/部署与环境配置.md](./doc/部署与环境配置.md)
- API 接口清单：[doc/API接口清单与示例.md](./doc/API接口清单与示例.md)
- 权限模型说明：[doc/权限模型说明.md](./doc/权限模型说明.md)
- 开发参考文档：[doc/开发参考文档.md](./doc/开发参考文档.md)

## 仓库说明

- 当前仓库以稳定演示、并发治理和自动化回归为主，不追求微服务拆分。
- 动态交易数据不会进入本地缓存，强一致性仍由数据库事务、条件更新和唯一索引保证。
- 如需继续扩展，建议围绕压测、可观测性和演示资产增强，而不是盲目堆叠中间件。
