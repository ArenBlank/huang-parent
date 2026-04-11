# 单体双实例与 Redis 并发落地说明

## 1. 部署形态
- `web-admin` 单实例，默认端口 `8080`
- `web-app` 双实例，默认端口 `8081` 和 `8082`
- Nginx 通过 `upstream app_backend` 轮询转发到两个 `web-app` 实例
- 认证继续使用 JWT，无需 session 粘滞

当前代码里的关键入口：
- 启动脚本：`start-all.ps1`
- 停止脚本：`stop-all.ps1`
- 宿主机 Nginx：`deploy/nginx/fitness.conf`
- Docker Nginx 模板：`docker/nginx/templates/default.conf.template`
- 生产环境样例：`docker/env/app.env.example`、`docker/env/app-instance-2.env.example`

为了让双实例验证更直观，`web-app` 响应头会回传 `X-App-Instance`，值形如 `web-app:8081` 或 `web-app:8082`。

## 2. Redis 在本项目里的职责边界

### 2.1 只做三类事情
- 读缓存：Banner、公告、系统配置、课程列表、训练计划静态详情
- 短状态：短信验证码、发送冷却、失败计数
- 跨实例辅助控制：热点缓存重建锁、重复提交短幂等键、支付回调短锁、固定窗口限流

### 2.2 不做的事情
- 不缓存订单详情、支付状态、退款状态
- 不缓存课程排期余量
- 不用 Redis 代替数据库做最终正确性判断
- 不引入 Seata、MQ 最终一致性、多级缓存

## 3. 三层并发策略

### 3.1 应用层
- 控制器入口做轻量限流与短幂等
- 相同用户重复点击报名/预约时，优先被 Redis 幂等键挡住
- 登录与短信发送走固定窗口限流，降低撞库与重试风暴

### 3.2 Redis 层
- `PlanBizService` 使用互斥锁保护热点缓存重建
- `PaymentCallbackBizService` 对同一 `payNo` 使用短锁，避免两个实例同时处理同一异步回调
- 报名和预约使用短幂等键，阻止前端重复点击造成的重复请求

### 3.3 数据库层
- 课程报名和教练预约都依赖原子 SQL 或条件更新防超卖
- 唯一索引继续作为重复报名、重复预约、重复回调的最终兜底
- 写链路保持事务，确保订单、支付、业务单状态一起推进

## 4. Redis Key 约定与 TTL

| 类型 | Key 前缀 | 默认 TTL | 说明 |
| --- | --- | --- | --- |
| Banner 缓存 | `app:banner:active` | 15 分钟 + 抖动 | 读多写少 |
| 公告缓存 | `app:notice:published:*` | 10 分钟 + 抖动 | 写后按前缀删 |
| 系统配置缓存 | `app:sys-config:*` | 60 分钟 + 抖动 | 支持空值缓存 |
| 课程列表缓存 | `app:course:list:*` | 10 分钟 + 抖动 | 不缓存排期余量 |
| 计划列表缓存 | `app:plan:list:active` | 10 分钟 + 抖动 | 静态列表 |
| 计划详情缓存 | `app:plan:detail:static:*` | 15 分钟 + 抖动 | 静态详情缓存 |
| 计划详情锁 | `app:plan:detail:lock:*` | 10 秒 | 缓存重建互斥 |
| 短信验证码 | `app:sms:code:*` | 10 分钟 | 验证成功即删除 |
| 短信冷却 | `app:sms:cooldown:*` | 60 秒 | 防止频繁发送 |
| 短信失败计数 | `app:sms:fail:*` | 10 分钟 | 辅助风控 |
| 登录限流 | `app:limit:login:*` / `admin:limit:login:*` | 60 秒 | 固定窗口 |
| 短信发送限流 | `app:limit:sms:send:*` | 10 分钟 | 固定窗口 |
| 报名限流 | `app:limit:course:enroll:*` | 30 秒 | 固定窗口 |
| 预约限流 | `app:limit:booking:create:*` | 30 秒 | 固定窗口 |
| 报名幂等 | `app:idem:course:enroll:*` | 5 秒 | 防重复点击 |
| 预约幂等 | `app:idem:booking:create:*` | 5 秒 | 防重复点击 |
| 支付回调短锁 | `app:guard:pay:callback:*` | 10 分钟 | 防多实例并发处理 |

## 5. 失败与降级策略
- 缓存读取失败：直接回源数据库，不阻断读请求
- 缓存写入失败：记录日志，下一次请求继续回源重建
- Redis 限流/幂等失败：默认放行，交由数据库继续兜底
- 支付回调短锁失效：数据库支付状态与唯一键仍然兜底
- Redis 不可用时：交易正确性仍以数据库事务、条件更新、唯一索引为准

## 6. 本项目里的并发概念映射

### 6.1 串行与并行
- 串行：单个请求线程内部按顺序执行校验、下单、写支付单
- 并行：两个 `web-app` 实例同时收到同一类请求

### 6.2 线程安全
- JVM 内的 `synchronized` 只能约束单实例内线程
- 双实例部署后，JVM 内锁无法覆盖另一个实例
- 所以跨实例互斥要靠 Redis 或数据库，而不是本地锁

### 6.3 什么时候用哪种手段
- 本地锁：只适合同一个 JVM 内的临界区学习，不适合线上双实例
- Redis 幂等/短锁：适合挡住重复点击、热点重建、短时间重复回调
- 数据库条件更新：适合名额、库存、状态推进这种最终正确性必须强保证的场景
- 唯一索引：适合做最终兜底，防止重复写入真的落库

## 7. 验证方式

### 7.1 双实例验证
```powershell
powershell -ExecutionPolicy Bypass -File start-all.ps1
powershell -ExecutionPolicy Bypass -File tests/check-dual-app-routing.ps1
```

如果本机还没把 `app.localhost` 配到 `127.0.0.1`，可直接使用脚本默认值
`http://localhost + Host: app.localhost` 完成验证；或者补一条本机 `hosts`：
- `127.0.0.1 app.localhost`
- `127.0.0.1 admin.localhost`
- `127.0.0.1 files.localhost`

预期：
- `X-App-Instance` 统计中同时出现 `web-app:8081` 和 `web-app:8082`
- JWT 请求经过 Nginx 转发后仍然可正常鉴权

### 7.2 并发/幂等验证
示例：
```powershell
$body = '{"scheduleId":1}'
powershell -ExecutionPolicy Bypass -File tests/invoke-concurrent-requests.ps1 `
  -Url "http://localhost/app/course/enroll" `
  -HostHeader "app.localhost" `
  -Method POST `
  -Body $body `
  -Token "<your-jwt-token>" `
  -Concurrency 10
```

观察重点：
- 相同请求并发下，只有极少数真正进入业务处理
- 重复点击优先返回“请勿重复提交”
- 即使 Redis 失效，数据库条件更新和唯一索引仍要保证不超卖、不重复写库

## 8. 学习建议
- 先跑双实例路由检查，确认部署骨架稳定
- 再跑并发脚本，观察 `X-App-Instance`、返回消息、数据库结果
- 最后结合日志里的 `traceId` 与业务日志，复盘一次完整链路

配套阅读：
- `doc/并发方案样板场景对比.md`
