# huang-parent

![CI](https://github.com/ArenBlank/huang-parent/actions/workflows/ci.yml/badge.svg?branch=V3)
![Integration](https://github.com/ArenBlank/huang-parent/actions/workflows/integration.yml/badge.svg?branch=V3)

本仓库为健身平台后端项目，包含 app/admin 两端与回归测试基线。

## 快速查看
- CI workflow：包含 `CI / build` 与 `CI / api-core-regression`
- Integration workflow：`Integration / api-regression`

## 稳定性亮点
- 端到端回归：Postman 核心/全量回归 + GitHub Actions 自动化验证
- 权限矩阵校验：启动时扫描 `@RequireAdminPermission` 与配置/数据库差异并告警
- 审计日志闭环：管理端关键操作写入 operation log，并由回归用例验证
- TraceId 链路追踪：每请求生成/透传 traceId，日志包含 traceId + userId + 耗时
- 回归数据重置：一键清理测试脏数据，保证可重复回归
