# k6 全链路压测脚本

## 环境要求

- k6 v1.7+（`k6 version`）
- Python 3 + `requests` 模块（`pip install requests`）
- 项目服务已启动（admin:8080 + app:8081），验证码已禁用

## 文件说明

| 文件 | 用途 | 峰值 VU |
|------|------|:---:|
| `01-cache-read.js` | 缓存读压测（Banner/Course/Plan，无需认证） | 500 |
| `02-enrollment-concurrent.js` | 课程报名并发（三层治理验证） | 100 |
| `03-mixed-workload.js` | 混合负载压测（80%读/20%写） | 80 |
| `generate-tokens.py` | 批量登录获取 JWT Token | — |

## 快速开始

### 1. 启动服务（验证码关闭）

```powershell
# admin
mvn -f web/web-admin/pom.xml -DskipTests spring-boot:run -Dspring-boot.run.arguments="--fitness.auth.captcha.enabled=false"

# app
mvn -f web/web-app/pom.xml -DskipTests spring-boot:run -Dspring-boot.run.arguments="--fitness.auth.captcha.enabled=false"
```

### 2. 准备测试数据

```sql
-- 设置排期容量
UPDATE course_schedule SET capacity = 20, booked_count = 0 WHERE id = 2;
DELETE FROM course_enrollment WHERE schedule_id = 2;
```

### 3. 生成 Token（需要约 3 分钟，登录接口有 10次/60s 限流）

```bash
cd scripts/load-test
python generate-tokens.py 30
```

### 4. 执行压测

```bash
# 第 1 轮：缓存读（无需 Token）
k6 run 01-cache-read.js

# 第 2 轮：课程报名并发（需先执行步骤 3）
k6 run 02-enrollment-concurrent.js

# 第 3 轮：混合负载（需先执行步骤 3）
k6 run 03-mixed-workload.js
```

### 5. 验证结果

```sql
-- 检查报名数 = 排期容量（无超卖）
SELECT COUNT(*) FROM course_enrollment WHERE schedule_id = 2 AND is_deleted = 0;
SELECT booked_count FROM course_schedule WHERE id = 2;
```

### 6. 清理压测数据

```sql
DELETE FROM course_enrollment WHERE schedule_id = 2 AND create_time >= NOW() - INTERVAL 1 HOUR;
UPDATE course_schedule SET booked_count = (SELECT COUNT(*) FROM (SELECT 1 FROM course_enrollment WHERE schedule_id = 2 AND is_deleted = 0) t) WHERE id = 2;
```

```bash
docker exec redis-container-huang redis-cli -a root --scan --pattern "app:limit:*" | xargs redis-cli -a root DEL
docker exec redis-container-huang redis-cli -a root --scan --pattern "app:idem:*" | xargs redis-cli -a root DEL
```

## 设计说明

### 三层并发治理验证逻辑（02-enrollment-concurrent.js）

```
请求 → @RateLimit（Redis INCR 固定窗口，5次/30s/用户）
     → @IdempotentSubmit（Redis SET NX 5s TTL）
     → Service Biz（booked_count < capacity 条件更新）
     → DB 唯一索引（最终兜底）
```

k6 脚本使用 30+ 独立用户 Token（SharedArray 随机抽取），100 VU 并发攻击同一排期。预期结果：

| 指标 | 预期 | 说明 |
|------|------|------|
| `enroll_success` | ≤ capacity | 成功数不应超过容量 |
| `enroll_rate_limited` | 占绝大多数 | @RateLimit 是第一道防线 |
| `enroll_duplicate` | 少量 | 5s 内同用户同排期重复 |
| `enroll_full` | capacity 耗尽后出现 | DB 层条件更新拦截 |

## 注意事项

1. 登录接口有 **10次/60s/IP** 的 @RateLimit，Token 生成脚本每批 10 个后会等待 65s
2. 压测前务必清理 Redis 中 `app:limit:*` 和 `app:idem:*` 的残留 key
3. 压测后 Token 会自动过期（2h），无需额外清理
4. `generate-tokens.sh` 为 bash 版本，`generate-tokens.py` 为 Python 版本，推荐使用 Python 版
