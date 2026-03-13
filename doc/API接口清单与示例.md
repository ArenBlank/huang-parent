# API接口清单与示例

## 1. 鉴权说明
- `/app/auth/**` 与 `/admin/auth/login` 为免登录接口。
- 其余 `/app/**`、`/admin/**` 接口默认需要 JWT。
- Header 示例：`Authorization: Bearer <accessToken>`

## 2. App端接口（核心）
1. `POST /app/auth/login`：用户登录
2. `POST /app/plan/subscribe`：订阅训练计划
3. `POST /app/record/checkin`：训练打卡
4. `GET /app/booking/schedule/list`：教练档期列表
5. `POST /app/booking/create`：创建预约订单
6. `POST /app/pay/callback`：支付回调（第三方异步通知）
7. `POST /app/pay/mock-notify`：支付回调（模拟）
8. `GET /app/plan/{planId}`：计划详情（含视频）
9. `GET /app/course/list`：课程列表
10. `POST /app/course/enroll`：课程报名并下单
11. `POST /app/course/refund`：课程退款申请
12. `GET /app/course/my/enrollments`：我的报名记录
13. `POST /app/coach/apply`：提交教练申请
14. `GET /app/coach/my-application`：我的教练申请
15. `GET /app/banner/list`：首页Banner列表
16. `GET /app/notice/list`：公告列表
17. `GET /app/system-config/map?keys=site_name&keys=xxx`：按key批量读配置

## 3. Admin端接口（核心）
1. `POST /admin/auth/login`：管理员登录
2. `GET /admin/dashboard/summary`：看板汇总
3. `GET /admin/ops/booking/list`：预约列表
4. `POST /admin/ops/booking/complete`：授课完成
5. `POST /admin/ops/booking/close-timeout`：关闭超时未支付预约
6. `POST /admin/video/upload`：上传视频到 MinIO
7. `POST /admin/video`：新增视频素材
8. `PUT /admin/video/bind-plan-item`：计划项绑定视频
9. `POST /admin/course`：创建课程
10. `PUT /admin/course/{id}/status`：上架/下架课程
11. `POST /admin/course/schedule`：创建课程排期
12. `GET /admin/role-permission/permissions`：权限清单
13. `GET /admin/role-permission/role/{roleId}/permissions`：角色权限列表
14. `POST /admin/role-permission/assign`：角色权限分配
15. `GET /admin/role/{roleId}/course-category-scope`：课程类目范围
16. `PUT /admin/role/{roleId}/course-category-scope`：更新课程类目范围
17. `PUT /admin/role/course-category-scope/batch`：批量更新课程类目范围
18. `GET /admin/refund/list`：退款审计列表
19. `GET /admin/pay/callback/list`：支付回调审计列表
20. `GET /admin/coach-apply/list`：教练申请列表
21. `POST /admin/coach-apply/audit`：教练申请审核
22. `GET /admin/operation-log/list`：操作日志列表
23. `GET /admin/banner/list`：Banner列表
24. `POST /admin/banner`：新增Banner
25. `PUT /admin/banner/{id}`：更新Banner
26. `PUT /admin/banner/{id}/status`：更新Banner状态
27. `DELETE /admin/banner/{id}`：删除Banner
28. `GET /admin/notice/list`：公告列表
29. `POST /admin/notice`：新增公告
30. `PUT /admin/notice/{id}`：更新公告
31. `PUT /admin/notice/{id}/status`：更新公告状态
32. `DELETE /admin/notice/{id}`：删除公告
33. `GET /admin/system-config/list`：系统配置列表
34. `POST /admin/system-config`：新增系统配置
35. `PUT /admin/system-config/{id}`：更新系统配置
36. `DELETE /admin/system-config/{id}`：删除系统配置

## 4. 快速联调顺序
1. 登录获取 app/admin token
2. app 订阅计划并打卡
3. app 创建预约，调用 mock 支付回调
4. admin 关闭超时单或完成授课
5. admin 上传视频并绑定计划项
6. app 查看计划详情，确认 `items[].video.playUrl`
7. app 课程报名、mock 支付、退款
8. 可选：切换为 HMAC 模式调用 `/app/pay/callback`
9. admin 查询退款审计与操作日志

## 5. 示例请求

### 5.1 用户登录
```bash
curl -X POST "http://localhost:8081/app/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"account\":\"member_chen\",\"password\":\"123456\",\"loginType\":\"password\"}"
```

### 5.2 上传视频到MinIO
```bash
curl -X POST "http://localhost:8080/admin/video/upload" \
  -H "Authorization: Bearer <adminToken>" \
  -F "file=@D:/videos/squat.mp4"
```

### 5.3 创建视频素材
```bash
curl -X POST "http://localhost:8080/admin/video" \
  -H "Authorization: Bearer <adminToken>" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\":\"深蹲教学-初级\",
    \"sourceSite\":\"pexels\",
    \"licenseType\":\"Pexels License\",
    \"durationSec\":45,
    \"tags\":\"squat,legs,beginner\",
    \"minioPath\":\"videos/upload/20260228/abc.mp4\",
    \"status\":1
  }"
```

### 5.4 查询操作日志
```bash
curl -X GET "http://localhost:8080/admin/operation-log/list?module=course&limit=20" \
  -H "Authorization: Bearer <adminToken>"
```

### 5.5 支付回调（HMAC验签示例）
payload 规则：`payNo|tradeNo|status|amount`  
签名：`HmacSHA256(payload, secret)` 的 Hex 值

```bash
curl -X POST "http://localhost:8081/app/pay/callback" \
  -H "Content-Type: application/json" \
  -d "{
    \"payNo\":\"PAY123\",
    \"tradeNo\":\"HMAC123456\",
    \"status\":\"SUCCESS\",
    \"amount\":199.00,
    \"sign\":\"<HMAC_HEX>\"
  }"
```
