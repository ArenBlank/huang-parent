# huang-parent

![CI](https://github.com/ArenBlank/huang-parent/actions/workflows/ci.yml/badge.svg?branch=V3)
![Integration](https://github.com/ArenBlank/huang-parent/actions/workflows/integration.yml/badge.svg?branch=V3)

本仓库为健身平台后端项目，包含 app/admin 两端与回归测试基线。

## 交付说明（收尾版）
- 当前状态：核心链路可演示（Admin + App），核心/全量回归均通过
- 演示路径（最短闭环）：
  - Admin：登录 -> 权限中心 -> 课程 -> 排期 -> 订单
  - App：登录 -> 计划订阅 -> 打卡 -> 课程报名/支付/退款 -> 预约/支付 -> 资料更新
- 常用账号（本地）：
  - Admin：`root_admin / root`
  - App：`member_chen / $2a$10$demoMemberPasswordHash`
- 复演建议：演示前执行一次
  - `powershell -ExecutionPolicy Bypass -File tests/reset-test-data.ps1`
- 详细收尾说明见：`doc/交付说明.md`

## 快速查看
- CI workflow：包含 `CI / build` 与 `CI / api-core-regression`
- Integration workflow：`Integration / api-regression`

## 稳定性亮点
- 端到端回归：Postman 核心/全量回归 + GitHub Actions 自动化验证
- 权限矩阵校验：启动时扫描 `@RequireAdminPermission` 与配置/数据库差异并告警
- 审计日志闭环：管理端关键操作写入 operation log，并由回归用例验证
- TraceId 链路追踪：每请求生成/透传 traceId，日志包含 traceId + userId + 耗时
- 回归数据重置：一键清理测试脏数据，保证可重复回归

## 演示路径（建议面试展示）
- 权限矩阵诊断：`GET /admin/permission/matrix`（审计管理员权限）
