# 单体双实例与 Redis 并发落地说明

## 1. 架构定位

本项目仍然是工程型单体，但部署层面已经不是“单 JVM 单实例”思路，而是：

- `web-admin`：单实例
- `web-app`：支持双实例
- `Nginx`：统一入口，轮询转发到多个 `web-app`

因此，这个项目在并发治理上需要同时考虑：

- 同一 JVM 内的线程安全
- 跨 JVM、跨实例的数据一致性
- Redis 不可用时的数据库兜底

## 2. 当前部署形态

- `web-admin` 默认端口：`8080`
- `web-app` 默认端口：`8081` / `8082`
- Nginx `upstream app_backend` 负责轮询分发
- JWT 无状态认证贯穿 App/Admin

关键入口：

- 启动脚本：`start-all.ps1`
- 停止脚本：`stop-all.ps1`
- 宿主机 Nginx：`deploy/nginx/fitness.conf`
- Docker Nginx 模板：`docker/nginx/templates/default.conf.template`
- 环境样例：`docker/env/app-instance-2.env.example`

为了便于验证双实例命中，`web-app` 响应头会返回 `X-App-Instance`。

## 3. Redis 在本项目中的职责边界

### 3.1 Redis 负责什么

- 读缓存：
  - Banner
  - Notice
  - SystemConfig
  - Course List
  - Plan List
  - Plan Detail Static
- 短状态：
  - 短信验证码
  - 短信发送冷却
  - 短信失败计数
- 跨实例辅助控制：
  - 固定窗口限流
  - 短幂等键
  - 支付回调短锁
  - 热点缓存重建锁
  - 任务执行锁
- 本地缓存失效广播：
  - Redis Pub/Sub `cache:evict`

### 3.2 Redis 不负责什么

- 不缓存订单详情、支付状态、退款状态
- 不缓存课程排期余量、预约余量
- 不替代数据库做最终正确性判断
- 不承接分布式事务
- 不引入 MQ、Seata、Redis Stream

## 4. 三层并发治理策略

### 4.1 应用层

- 使用 `@RateLimit` 控制登录、短信发送、敏感写接口频率
- 使用 `@IdempotentSubmit` 阻断重复点击和短时间重试
- 控制器入口优先挡掉明显无效请求

### 4.2 Redis 层

- 计划详情缓存使用互斥锁防止热点击穿
- 支付回调对同一 `payNo` 加短锁，避免多实例同时推进状态
- 报名、预约使用短幂等键挡住重复提交
- 任务中心对同一任务使用短锁避免多实例重复执行

### 4.3 数据库层

- 报名、预约依赖条件更新防超卖
- 写链路全部保留事务
- 唯一索引作为重复报名、重复预约、重复回调的最终兜底

## 5. 多级缓存与近实时失效

### 5.1 当前多级缓存结构

- `L1`：Caffeine 本地缓存
- `L2`：Redis 共享缓存
- `DB`：最终数据源

### 5.2 当前只允许进入本地缓存的数据

- Banner
- Notice
- SystemConfig
- Course List
- Plan List
- Plan Detail Static

### 5.3 当前失效策略

事务提交后执行：

1. 删除 Redis 共享缓存
2. 删除当前实例本地 Caffeine
3. 发布 `cache:evict` 到 Redis Pub/Sub
4. 其他实例订阅后仅删除自己的本地缓存

说明：

- `afterCommit` 包住整个 `sharedEvict`
- Subscriber 不会再次删除 Redis
- Subscriber 不会回发消息
- TTL 继续保留，作为漏消息时的最终兜底

### 5.4 明确不进入本地缓存的数据

- 订单详情
- 支付状态
- 退款状态
- 课程排期余量
- 预约状态
- 用户动态资料态

## 6. JWT 与多实例认证

项目当前使用 `TokenVersion-enhanced stateless JWT`：

- token 中携带：
  - `platform`
  - `type`
  - `tokenVersion`
- App/Admin 鉴权统一校验：
  - user 是否存在
  - `status == 1`
  - token 中的 `tokenVersion` 是否和数据库一致

当前收益：

- 双实例下无状态鉴权
- 密码重置后旧 token 立即失效
- 账号禁用后旧 token 立即失效
- App/Admin refresh token 可按平台正确续签

## 7. 典型场景落地

### 7.1 课程报名

- 应用层：限流 + 短幂等
- Redis：挡重复点击
- DB：`booked_count < capacity`
- DB 唯一索引：最终兜底防重复落库

### 7.2 教练预约

- 应用层：限流 + 短幂等
- Redis：挡重复提交
- DB：条件更新控制档期占用
- DB 唯一索引：最终兜底

### 7.3 支付回调

- Redis 短锁防多实例同时处理
- 支付状态校验防重复推进
- 回调审计日志记录处理结果
- DB 幂等键/唯一约束作为最终兜底

### 7.4 计划详情缓存

- 静态详情可缓存
- 用户订阅态实时查库
- `playUrl` 实时生成
- 热点 key 通过互斥锁控制缓存重建

## 8. 失败与降级策略

- 缓存读取失败：回源数据库
- 缓存写入失败：记录日志，后续请求继续重建
- `@RateLimit` Redis 异常：按 Fail-Open 处理，避免把所有请求一刀切阻断
- `@IdempotentSubmit` Redis 异常：按 Fail-Close 处理，避免重复提交直接穿透
- 分布式任务锁降级：默认跳过执行并写入任务日志，避免双实例重复跑任务
- 支付回调锁降级：允许继续进入数据库幂等链路，最终一致性仍由条件更新和状态校验兜底
- 支付回调短锁失效：数据库状态与唯一索引继续兜底
- Pub/Sub 消息漏收：TTL 最终兜底恢复正确
- Redis 不可用时：交易链路仍以数据库事务、条件更新、唯一索引为准

## 9. 验证方式

### 9.1 双实例验证

```powershell
powershell -ExecutionPolicy Bypass -File start-all.ps1
powershell -ExecutionPolicy Bypass -File tests/check-dual-app-routing.ps1
```

预期：

- `X-App-Instance` 同时命中 `web-app:8093` 和 `web-app:8094`
- JWT 请求经过 Nginx 转发后仍能正常鉴权

### 9.2 并发验证

```powershell
$body = '{"scheduleId":1}'
powershell -ExecutionPolicy Bypass -File tests/invoke-concurrent-requests.ps1 `
  -Url "http://localhost/app/course/enroll" `
  -HostHeader "app.localhost" `
  -Method POST `
  -Body $body `
  -Token "<jwt>" `
  -Concurrency 10
```

观察点：

- 重复请求大部分会被幂等或限流提前挡住
- 报名、预约不会超卖
- 支付回调不会重复入账

### 9.3 当前已实测通过

- 双实例轮询通过
- JWT 跨实例访问通过
- 课程报名并发验证通过
- 教练预约并发验证通过
- 支付回调重复投递幂等验证通过
- `Caffeine + Redis + Redis Pub/Sub` 近实时失效验证通过
