# API 接口清单与示例

## 1. 鉴权说明

- `/app/auth/**` 与 `/admin/auth/login` 为免登录接口
- `/admin/auth/refresh-token` 使用 refresh token 调用，不依赖现有 access token
- 其余 `/app/**`、`/admin/**` 接口默认需要 JWT
- Header 示例：`Authorization: Bearer <accessToken>`

## 2. App 端核心接口

### 2.1 认证

- `POST /app/auth/sms-code/send`
- `POST /app/auth/register`
- `POST /app/auth/login`
- `POST /app/auth/refresh-token`
- `POST /app/auth/forget-password`
- `POST /app/auth/logout`

### 2.2 训练计划与打卡

- `GET /app/plan/list`
- `GET /app/plan/{planId}`
- `POST /app/plan/subscribe`
- `POST /app/record/checkin`
- `GET /app/record/my/list`
- `GET /app/record/my/weekly-stat`

### 2.3 教练预约

- `GET /app/booking/schedule/list`
- `POST /app/booking/create`
- `POST /app/booking/pay-success`
- `POST /app/booking/complete`
- `POST /app/booking/review`
- `GET /app/booking/my/list`

### 2.4 课程学习

- `GET /app/course/list`
- `GET /app/course/{courseId}/schedule/list`
- `POST /app/course/enroll`
- `POST /app/course/pay-success`
- `POST /app/course/cancel-unpaid`
- `POST /app/course/refund`
- `GET /app/course/my/enrollments`

### 2.5 支付与订单

- `POST /app/pay/callback`
- `POST /app/pay/mock-notify`
- `GET /app/order/my/list`
- `GET /app/order/detail`

### 2.6 个人资料与运营内容

- `GET /app/profile/info`
- `PUT /app/profile/info`
- `POST /app/profile/avatar/upload`
- `PUT /app/profile/password`
- `POST /app/coach/apply`
- `GET /app/coach/my-application`
- `GET /app/banner/list`
- `GET /app/notice/list`
- `GET /app/system-config/map`

## 3. Admin 端核心接口

### 3.1 认证与看板

- `POST /admin/auth/login`
- `POST /admin/auth/refresh-token`
- `GET /admin/dashboard/summary`

### 3.2 任务中心

- `GET /admin/task-run/page`
- `GET /admin/task-run/summary`
- `POST /admin/task-run/{taskCode}/trigger`

### 3.3 预约与订单运营

- `GET /admin/ops/booking/list`
- `POST /admin/ops/booking/complete`
- `POST /admin/ops/booking/close-timeout`
- `GET /admin/ops/order/list`
- `GET /admin/ops/order/detail`

### 3.4 课程管理

- `GET /admin/course/list`
- `POST /admin/course`
- `PUT /admin/course/{id}`
- `PUT /admin/course/{id}/status`
- `GET /admin/course/schedule/list`
- `POST /admin/course/schedule`
- `PUT /admin/course/schedule/{id}/status`
- `GET /admin/course-category/list`

### 3.5 训练计划与视频素材

- `GET /admin/training-plan/list`
- `GET /admin/training-plan/{planId}`
- `POST /admin/training-plan`
- `PUT /admin/training-plan/{id}`
- `DELETE /admin/training-plan/{id}`
- `POST /admin/training-plan/{planId}/item`
- `PUT /admin/training-plan/item/{itemId}`
- `DELETE /admin/training-plan/item/{itemId}`
- `GET /admin/video/list`
- `POST /admin/video/upload`
- `POST /admin/video`
- `PUT /admin/video/{id}`
- `PUT /admin/video/{id}/status`
- `DELETE /admin/video/{id}`
- `PUT /admin/video/bind-plan-item`
- `PUT /admin/video/unbind-plan-item/{planItemId}`

### 3.6 运营内容

- `GET /admin/banner/list`
- `POST /admin/banner`
- `PUT /admin/banner/{id}`
- `PUT /admin/banner/{id}/status`
- `DELETE /admin/banner/{id}`
- `GET /admin/notice/list`
- `POST /admin/notice`
- `PUT /admin/notice/{id}`
- `PUT /admin/notice/{id}/status`
- `DELETE /admin/notice/{id}`
- `GET /admin/system-config/list`
- `POST /admin/system-config`
- `PUT /admin/system-config/{id}`
- `DELETE /admin/system-config/{id}`

### 3.7 审计与审核

- `GET /admin/refund/list`
- `GET /admin/pay/callback/list`
- `GET /admin/coach-apply/list`
- `GET /admin/coach-apply/{profileId}`
- `POST /admin/coach-apply/audit`
- `GET /admin/operation-log/list`

### 3.8 权限与用户角色

- `GET /admin/permission/matrix`
- `POST /admin/permission/matrix/sync`
- `GET /admin/role-permission/permissions`
- `GET /admin/role-permission/role/{roleId}/permissions`
- `POST /admin/role-permission/assign`
- `GET /admin/role/list`
- `GET /admin/role/detail/{roleId}`
- `PUT /admin/role/status`
- `GET /admin/role/available`
- `GET /admin/role/{roleId}/course-category-scope`
- `PUT /admin/role/{roleId}/course-category-scope`
- `PUT /admin/role/course-category-scope/batch`
- `GET /admin/user/list`
- `GET /admin/user/detail/{userId}`
- `PUT /admin/user/status`
- `POST /admin/user/assign-roles`
- `GET /admin/user-role/list`
- `GET /admin/user-role/user/{userId}/roles`
- `GET /admin/user-role/role/{roleId}/users`
- `POST /admin/user-role/batch-assign`
- `DELETE /admin/user-role/{userId}/role/{roleId}`

## 4. 典型接口示例

### 4.1 App 登录

```http
POST /app/auth/login
Content-Type: application/json

{
  "account": "root_member",
  "password": "root",
  "loginType": "password"
}
```

### 4.2 Admin 刷新令牌

```http
POST /admin/auth/refresh-token
Content-Type: application/json

{
  "refreshToken": "eyJ..."
}
```

### 4.3 课程报名

```http
POST /app/course/enroll
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "scheduleId": 1
}
```

### 4.4 手动触发任务

```http
POST /admin/task-run/BOOKING_TIMEOUT_CLOSE/trigger
Authorization: Bearer <adminAccessToken>
```

## 5. 当前接口特点

- App/Admin 统一走 JWT
- App/Admin 都支持 refresh token
- 敏感写接口已接入限流/短幂等
- App 读接口中的静态低频内容已接入多级缓存
- Admin 写接口中的缓存失效链路已接入 Redis Pub/Sub 广播

## 6. 推荐查看方式

- 接口调试：Knife4j
- 自动化验证：Postman / Newman
- 双实例验证：`tests/check-dual-app-routing.ps1`
- 并发验证：`tests/invoke-concurrent-requests.ps1`
