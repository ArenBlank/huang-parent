# 智训健身平台 k6 全链路压测方案

> **设计方**: Claude（方案设计）  
> **执行方**: DeepSeek（脚本编写 + 执行 + 结果分析）  
> **测试工具**: k6  
> **目标项目**: huang-parent (Spring Boot 3 单体双实例 + MySQL + Redis + Nginx)

---

## 0. 前置：本项目与 VelocityMall 的关键差异

| 维度 | VelocityMall (参考) | huang-parent (本项目) |
|------|:---:|:---:|
| 网关层 | Spring Cloud Gateway + Sentinel 限流/熔断 | **无网关**，直接 Nginx → Spring Boot |
| 异步削峰 | RocketMQ | **无 MQ**，同步处理 |
| DB 连接池 | 未明确限制 | **HikariCP max 12/实例**（双实例共 24） |
| 限流机制 | Sentinel 全局 5 QPS/SKU | `@RateLimit` **按用户维度**限流（5次/30s） |
| 幂等机制 | Redis Lua SISMEMBER | `@IdempotentSubmit` SET NX 5s TTL |
| 缓存 | 无本地缓存 | Caffeine + Redis + Pub/Sub 多级缓存 |
| 部署 | 微服务多 JAR | **单体双实例**（8081/8082）Nginx 轮询 |
| 线程池 | 默认 | Spring Boot 默认（Tomcat 200 线程） |

**核心结论**：本项目没有网关层削峰，所有请求直达 Spring Boot。HikariCP max=12 是主要瓶颈。缓存的读接口应该扛得住较高 QPS，但写接口和直接查 DB 的读接口会因为连接池受限。

---

## 1. 压测架构

```
k6 VUs → Nginx (80, 轮询)
              ├── web-admin:8080 (单实例)
              └── upstream app_backend
                    ├── web-app:8081
                    └── web-app:8082
                          ↓
                    Redis (6379) ← 限流/幂等/缓存/锁
                    MySQL (3306) ← HikariCP max=12/实例
```

**被测组件**：

| 组件 | 端口 | 说明 |
|------|------|------|
| Nginx | 80 | 反向代理 + 轮询负载均衡 |
| web-admin | 8080 | 管理端（单实例） |
| web-app-1 | 8081 | 用户端实例一 |
| web-app-2 | 8082 | 用户端实例二 |
| Redis | 6379 | 限流窗口计数、幂等键、缓存、分布式锁 |
| MySQL | 3306 | HikariCP max=12/实例，共 24 |

**关键瓶颈预估**：

| 瓶颈点 | 限制值 | 影响 |
|--------|--------|------|
| HikariCP | 12 连接/实例 | DB 查询并发上限 ~24 |
| Tomcat 线程 | 默认 200/实例 | HTTP 请求并发上限 ~400 |
| @RateLimit | 5次/30s/用户 | 单用户写操作被限制 |
| @IdempotentSubmit | SET NX 5s | 同用户同资源 5s 内重复请求被拒 |
| Redis 连接池 | Lettuce 默认 | 通常不是瓶颈 |

---

## 2. 压测准备

### 2.1 环境确认

```bash
# 确认所有服务已启动
curl -s http://localhost:8080/v3/api-docs | head -c 200
curl -s http://localhost:8081/v3/api-docs | head -c 200
curl -s http://localhost:8082/v3/api-docs | head -c 200
curl -s http://localhost/app/banner/list -H "Host: app.localhost" | head -c 200

# 确认 Redis 可达
redis-cli -a root PING

# 确认 MySQL 可达
mysql -h 127.0.0.1 -P 3306 -uroot -proot -e "SELECT 1"
```

### 2.2 重置测试数据

```powershell
powershell -ExecutionPolicy Bypass -File tests/reset-test-data.ps1
```

### 2.3 测试用户准备

本项目已有测试用户，通过 Postman 集合可知：
- **App 端**：`testuser / 123456`（dev 环境跳过短信验证）
- **Admin 端**：`testuser / 123456`（dev 环境跳过验证码）

为多用户压测，需要批量准备 100-200 个不同的测试账号（或使用已有账号的不同 phone）。

**生成脚本**：见附录 A。

### 2.4 压测数据准备

针对课程报名压测，需要确保：
- 课程排期存在（`course_schedule` 表，scheduleId=1 或 2）
- 排期 `booked_count < capacity`（容量充足）
- 教练档期存在（`coach_schedule` 表）

---

## 3. 压测场景设计

### 第 1 轮：缓存读——Banner/Course/Plan 高频读取

**目的**：验证 Caffeine + Redis 多级缓存吞吐能力，测量纯缓存读的 QPS 上限。

**目标接口**：
- `GET /app/banner/list` — Banner 缓存（Caffeine 30s / Redis 15min）
- `GET /app/course/list` — 课程列表缓存（Caffeine 15s / Redis 10min）
- `GET /app/plan/overview` — 训练计划概览缓存（Caffeine 15s / Redis 10min）

**特点**：这些接口命中 Caffeine 本地缓存时**不经过 DB**，理论上 QPS 最高。

```
stages: [
  { duration: '10s', target: 100  },   // 10s 拉升到 100 VU
  { duration: '30s', target: 300  },   // 30s 拉升到 300 VU
  { duration: '30s', target: 500  },   // 30s 保持 500 VU
  { duration: '10s', target: 0    },   // 10s 回落
]
// 总时长: 80s, 峰值 VU: 500, 无需 Token
```

**观察指标**：
- QPS 上限
- P50/P95/P99 延迟
- Nginx 两个实例的请求分布
- Redis 命中率（通过日志推断）

---

### 第 2 轮：DB 读——非缓存数据查询

**目的**：直接查 DB 的接口（绕过缓存），测量 HikariCP 12 连接上限下的最大吞吐。

**目标接口**（均需 JWT Token）：
- `GET /app/course/my/enrollments` — 我的报名记录（查 `course_enrollment` 表）
- `GET /app/booking/my/list` — 我的预约列表（查 `coach_booking` 表）
- `GET /app/order/my/list` — 我的订单列表（查 `order_info` 表）

**特点**：每次都查 DB，HikariCP 连接数 12×2=24 是硬上限。

```
stages: [
  { duration: '10s', target: 50   },   // 10s 拉升到 50 VU
  { duration: '30s', target: 100  },   // 30s 拉升到 100 VU
  { duration: '20s', target: 100  },   // 20s 保持 100 VU
  { duration: '10s', target: 0    },   // 10s 回落
]
// 总时长: 70s, 峰值 VU: 100, 需要 Token (多用户轮换)
```

**关键观察**：
- HikariCP `active_connections` 是否达到 12
- 是否有 `Connection is not available` 错误
- 响应时间是否因连接等待而飙升
- P95 延迟是否能维持在合理范围

---

### 第 3 轮：高冲突写——课程报名并发（核心）

**目的**：验证课程报名三层并发治理（`@IdempotentSubmit` + `@RateLimit` + DB 条件更新 + 唯一索引兜底）。

**目标接口**：
- `POST /app/course/enroll` — 课程报名
  - `@IdempotentSubmit`: prefix=`app:idem:course:enroll:{userId}:{scheduleId}`, ttl=5s
  - `@RateLimit`: prefix=`app:limit:course:enroll:{userId}`, 5次/30s

**前置准备**：
1. 确认测试排期 `scheduleId` 且 `capacity > 0`
2. 准备 50-100 个不同用户 Token
3. 设置排期容量为有限值（如 50）

**压测配置**：
```
stages: [
  { duration: '5s',  target: 200  },   // 5s 尖峰拉升
  { duration: '15s', target: 200  },   // 15s 保持
  { duration: '5s',  target: 0    },   // 5s 回落
]
// 总时长: 25s, 峰值 VU: 200, 多用户随机 Token (SharedArray)
```

**预期行为**：
| 结果 | 含义 | 预期占比 |
|------|------|:---:|
| 报名成功 | 首次请求 + 容量足 + SQL 更新成功 | ≤ 容量数 |
| "请勿重复提交报名请求" | 5s 内同一用户同一排期重复请求 | 少量 |
| "报名请求过于频繁" | 同一用户 30s 内超过 5 次 | 少量（取决于 VU 是否重复抽取同一用户） |
| 排期已满 | 并发竞争下 capacity 耗尽 | 余量 |
| 校验失败/参数错误 | scheduleId 无效或课程状态异常 | 0% |

**验证**：
```sql
-- 报名成功数 = capacity（无超卖）
SELECT COUNT(*) FROM course_enrollment WHERE schedule_id = <scheduleId> AND is_deleted = 0;
-- booked_count = 报名成功数
SELECT booked_count FROM course_schedule WHERE id = <scheduleId>;
```

---

### 第 4 轮：高冲突写——教练预约并发

**目的**：验证教练预约的三层并发治理。

**目标接口**：
- `POST /app/booking/create` — 创建预约
  - `@IdempotentSubmit`: prefix=`app:idem:booking:create:{userId}:{scheduleId}`, ttl=5s
  - `@RateLimit`: prefix=`app:limit:booking:create:{userId}`, 5次/30s

**压测配置**：同第 3 轮，200 VU，多用户随机 Token。

**预期行为**：同第 3 轮。每个档期只能被预约一次（或按 capacity）。

---

### 第 5 轮：登录限流验证

**目的**：验证登录接口的双重 `@RateLimit`（IP + 账号维度）。

**目标接口**：
- `POST /app/auth/login` — App 登录
  - `@RateLimit` x2: IP 维度 10次/60s + 账号维度 10次/60s

**子场景 5a — 同 IP 不同账号**：
```
stages: [
  { duration: '5s', target: 50 },   // 5s 拉升到 50 VU
  { duration: '10s', target: 50 },  // 10s 保持 50 VU
  { duration: '5s', target: 0  },   // 5s 回落
]
// 全部从同一 IP 发送（k6 默认），不同账号
// 预期: IP 限流触发，大量 "Login requests are too frequent"
```

**子场景 5b — 同一账号**：
```
stages: [
  { duration: '5s', target: 30 },   // 5s 拉升
  { duration: '10s', target: 30 },  // 10s 保持
  { duration: '5s', target: 0  },   // 5s 回落
]
// 全部使用同一账号 + 同一 IP
// 预期: IP + 账号双重限流，几乎全部被拒
```

---

### 第 6 轮：混合场景——模拟真实流量

**目的**：模拟真实用户行为，验证系统在混合读写下的稳定性。

**流量配比**（模拟一个用户打开 App 后的行为链）：

| 接口 | 占比 | 说明 |
|------|:---:|------|
| `GET /app/banner/list` | 15% | 首页 Banner（缓存） |
| `GET /app/course/list` | 15% | 课程列表（缓存） |
| `GET /app/plan/overview` | 10% | 训练计划概览（缓存） |
| `GET /app/course/my/enrollments` | 10% | 我的课程（DB） |
| `GET /app/booking/my/list` | 10% | 我的预约（DB） |
| `GET /app/order/my/list` | 10% | 我的订单（DB） |
| `GET /app/booking/schedule/summary` | 10% | 教练档期查询（DB） |
| `POST /app/course/enroll` | 10% | 课程报名（写，限流） |
| `POST /app/booking/create` | 5% | 教练预约（写，限流） |
| `POST /app/auth/refresh-token` | 5% | Token 刷新 |

**压测配置**：
```
stages: [
  { duration: '10s', target: 50  },   // 慢拉升
  { duration: '60s', target: 150 },   // 保持 150 VU 60s
  { duration: '10s', target: 0   },   // 回落
]
// 总时长: 80s, 峰值 VU: 150, 多用户 Token
```

**关键观察**：
- 混合场景下的 P95 延迟是否超过 500ms
- HikariCP 连接池是否饱和
- 是否有连接超时
- 限流/幂等拦截是否正确
- 双实例负载是否均匀

---

### 第 7 轮（可选）：持久负载——长时稳定性

**目的**：验证长时间运行的稳定性，发现内存泄漏、连接泄漏、缓存膨胀等问题。

```
stages: [
  { duration: '30s', target: 50  },   // 慢拉升
  { duration: '5m',  target: 100 },   // 保持 100 VU 5 分钟
  { duration: '30s', target: 0   },   // 回落
]
// 总时长: 6min
```

---

### 第 8 轮（可选）：混沌——Redis 降级验证

**目的**：验证 Redis 不可用时 `@RateLimit(failOpen=true)` 和 `@IdempotentSubmit(failOpen=false)` 的降级行为。

**操作**：压测进行中手动停止 Redis 容器，观察：
- 限流是否降级放行（failOpen=true → 请求继续处理）
- 幂等是否降级拒绝（failOpen=false → 返回 SERVICE_ERROR）

```
stages: [
  { duration: '10s', target: 200 },   // 拉升
  { duration: '30s', target: 200 },   // 保持（中途停 Redis）
  { duration: '10s', target: 0   },   // 回落
]
```

**注意**：此轮需要人工介入（停 Redis），可选执行。

---

## 4. 压测数据清理方案（重要）

### 4.1 k6 脚本内置清理

每个压测脚本的 `teardown()` 函数必须包含清理逻辑。

### 4.2 MySQL 清理

```sql
-- 清理压测产生的测试用户（按注册手机号前缀识别）
DELETE FROM user WHERE phone LIKE '1990000%' AND is_deleted = 0;
DELETE FROM user_profile WHERE user_id NOT IN (SELECT id FROM user WHERE is_deleted = 0);

-- 清理压测产生的报名记录
DELETE FROM course_enrollment WHERE user_id IN (
  SELECT id FROM user WHERE phone LIKE '1990000%'
);

-- 清理压测产生的预约记录
DELETE FROM coach_booking WHERE user_id IN (
  SELECT id FROM user WHERE phone LIKE '1990000%'
);

-- 清理压测产生的订单
DELETE oi FROM order_item oi
  INNER JOIN order_info o ON oi.order_id = o.id
  WHERE o.user_id IN (SELECT id FROM user WHERE phone LIKE '1990000%');

DELETE FROM order_info WHERE user_id IN (
  SELECT id FROM user WHERE phone LIKE '1990000%'
);

-- 清理压测产生的支付记录
DELETE FROM payment_record WHERE order_id NOT IN (SELECT id FROM order_info);

-- 清理压测产生的退款记录
DELETE FROM refund_record WHERE order_id NOT IN (SELECT id FROM order_info);

-- 清理压测产生的支付回调日志
DELETE FROM payment_callback_log WHERE pay_no LIKE 'MOCK%' OR pay_no LIKE 'TEST%';

-- 清理操作日志
DELETE FROM operation_log WHERE operator_id IN (
  SELECT id FROM user WHERE phone LIKE '1990000%'
);

-- 重置被修改的排期容量
-- （根据实际压测中使用的 schedule_id 调整）
UPDATE course_schedule SET booked_count = 0 WHERE id = <压测排期ID>;
UPDATE coach_schedule SET status = 1 WHERE id = <压测档期ID>;
```

### 4.3 Redis 清理

```bash
# 压测结束后执行，清理所有压测相关的 Redis key
redis-cli -a root --scan --pattern "app:limit:*" | xargs redis-cli -a root DEL
redis-cli -a root --scan --pattern "app:idem:*" | xargs redis-cli -a root DEL
redis-cli -a root --scan --pattern "app:login:*" | xargs redis-cli -a root DEL
redis-cli -a root --scan --pattern "app:sms:*" | xargs redis-cli -a root DEL
redis-cli -a root --scan --pattern "app:guard:*" | xargs redis-cli -a root DEL
redis-cli -a root --scan --pattern "app:banner:*" | xargs redis-cli -a root DEL
redis-cli -a root --scan --pattern "app:course:*" | xargs redis-cli -a root DEL
redis-cli -a root --scan --pattern "app:plan:*" | xargs redis-cli -a root DEL
redis-cli -a root --scan --pattern "app:notice:*" | xargs redis-cli -a root DEL
redis-cli -a root --scan --pattern "app:sys-config:*" | xargs redis-cli -a root DEL
redis-cli -a root --scan --pattern "task:lock:*" | xargs redis-cli -a root DEL

# 清理缓存失效通道残留
redis-cli -a root DEL "cache:evict"
```

### 4.4 清理脚本化

将上述 SQL + Redis 命令整合为一个 `tests/cleanup-after-load-test.sh`（或 `.ps1`），压测结束后一键执行。

---

## 5. 预期 QPS 估算（供参考，实际以压测结果为准）

| 场景 | 预估 QPS | 瓶颈 |
|------|:---:|------|
| 缓存读（Banner/Course/Plan） | 2000-5000+ | Tomcat 线程 / 网络 |
| DB 读（My Enrollments/Orders） | 100-300 | HikariCP max=24 |
| 课程报名（写） | 50-150 | HikariCP + Redis 限流 |
| 教练预约（写） | 50-150 | 同上 |
| 登录 | 10/用户/分钟 | @RateLimit 按用户硬限制 |
| 混合场景 | 200-800 | HikariCP 为主瓶颈 |

**注意**：以上是保守估计。本项目没有全局限流器（如 Sentinel），多个不同用户并发写操作可以穿透限流，真正的瓶颈在 HikariCP 12 连接。如果压测结果远低于预估，首先检查 HikariCP 活跃连接数。

---

## 6. k6 脚本清单

| 文件名 | 对应轮次 | 说明 |
|--------|:---:|------|
| `scripts/load-test/01-cache-read-test.js` | 第 1 轮 | 缓存读 500 VU |
| `scripts/load-test/02-db-read-test.js` | 第 2 轮 | DB 读 100 VU + 多用户 Token |
| `scripts/load-test/03-course-enroll-test.js` | 第 3 轮 | 课程报名 200 VU 并发 |
| `scripts/load-test/04-booking-create-test.js` | 第 4 轮 | 教练预约 200 VU 并发 |
| `scripts/load-test/05-login-rate-limit-test.js` | 第 5 轮 | 登录限流验证 |
| `scripts/load-test/06-mixed-workload-test.js` | 第 6 轮 | 混合场景 150 VU |
| `scripts/load-test/07-endurance-test.js` | 第 7 轮 | 持久负载 5min |
| `scripts/load-test/08-chaos-redis-down-test.js` | 第 8 轮 | 混沌-Redis 降级 |
| `scripts/load-test/helpers/user-token-generator.js` | 辅助 | 批量获取/刷新 Token |
| `scripts/load-test/cleanup.sh` | 清理 | 压测后清理 MySQL + Redis |

---

## 7. 执行顺序（建议）

```
1. 启动所有服务 + 重置测试数据
2. 执行第 1 轮（缓存读）       ← 最容易，先确认基础能力
3. 执行第 2 轮（DB 读）         ← 测量 HikariCP 上限
4. 执行第 5 轮（登录限流）      ← 验证限流正确性
5. 执行第 3 轮（课程报名）      ← 核心写场景
   └── 3a: 先从小 VU 开始（50 VU），确认逻辑正确
   └── 3b: 再放大到 200 VU
6. 执行第 4 轮（教练预约）      ← 核心写场景
7. 执行第 6 轮（混合负载）      ← 真实场景模拟
8. 执行第 7 轮（持久负载）      ← 稳定性验证（可选）
9. 执行第 8 轮（混沌降级）      ← 可选，需人工介入
10. 清理压测数据 ← 必须执行
```

每轮压测后建议等待 60s，让 Redis 限流窗口计数器自然过期。

---

## 8. 附录 A：批量 Token 生成策略

由于本项目使用 JWT 无状态认证 + TokenVersion，多用户压测有两种方案：

**方案 A（推荐）**：直接使用测试数据库中已有的用户，批量登录获取 Token。

```javascript
// k6 setup 阶段：批量登录获取 Token
export function setup() {
  const tokens = [];
  const users = [
    { account: 'testuser', password: '123456' },
    { account: '13800000001', password: '123456' },
    // ... 更多测试用户
  ];
  for (const user of users) {
    const res = http.post('http://localhost/app/auth/login', JSON.stringify({
      account: user.account,
      password: user.password,
      loginType: 'password'
    }), { headers: { 'Content-Type': 'application/json' } });
    if (res.status === 200) {
      const body = JSON.parse(res.body);
      if (body.data && body.data.accessToken) {
        tokens.push(body.data.accessToken);
      }
    }
  }
  return { tokens };
}
```

**方案 B**：压测前用脚本批量注册新用户（phone: 19900000001 ~ 19900000100），压测结束后清理。

---

## 9. 附录 B：k6 关键配置模板

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';

// 自定义指标
const successRate = new Rate('success');
const cacheHitRate = new Rate('cache_hit');
const rateLimitedRate = new Rate('rate_limited');
const idempotentRate = new Rate('idempotent_blocked');
const responseTime = new Trend('response_time_ms');

// 加载多用户 Token
const tokens = new SharedArray('tokens', function () {
  return JSON.parse(open('../data/test-tokens.json'));
});

export const options = {
  // 按场景配置 stages
  thresholds: {
    'http_req_duration': ['p(95)<1000'],   // P95 < 1s
    'http_req_failed': ['rate<0.05'],       // 失败率 < 5%
  },
};

export default function () {
  const token = tokens[Math.floor(Math.random() * tokens.length)];
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
    'Host': 'app.localhost',
  };

  const res = http.get('http://localhost/app/course/my/enrollments', { headers });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response has data': (r) => {
      try { const b = JSON.parse(r.body); return b.code === 200; } catch (e) { return false; }
    },
  });

  responseTime.add(res.timings.duration);
  successRate.add(res.status === 200);

  sleep(0.1); // 防端口耗尽
}
```

---

## 10. 给 DeepSeek 的执行指令

1. 阅读本方案全文，理解每轮压测的目的和预期行为
2. 根据附录 B 模板编写所有 k6 脚本
3. 先执行第 1 轮（缓存读），确认脚本正确 + 服务可达
4. 每轮压测后记录：总请求数、QPS、P50/P95/P99、失败率、错误分类
5. 第 3/4 轮（写）压测后，执行 SQL 验证无超卖
6. 全部完成后，执行 cleanup.sh 清理数据
7. 汇总生成 `k6-load-test-report.md` 压测报告
