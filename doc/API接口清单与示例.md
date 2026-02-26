# API接口清单与示例

## 1. 鉴权说明
- 除 `/app/auth/**` 外，`/app/**` 接口都需要 JWT。
- Header 示例：`Authorization: Bearer <accessToken>`

## 2. App 端接口（已实现）
1. `GET /app/plan/list`：训练计划列表
2. `GET /app/plan/{planId}`：训练计划详情（含动作视频 `playUrl`）
3. `POST /app/plan/subscribe`：订阅训练计划
4. `POST /app/record/checkin`：训练打卡
5. `GET /app/record/my/list`：我的打卡记录
6. `GET /app/record/my/weekly-stat`：近7日训练统计
7. `GET /app/booking/schedule/list`：教练档期列表
8. `POST /app/booking/create`：创建预约+订单
9. `POST /app/booking/pay-success`：支付成功回调（模拟）
10. `POST /app/booking/complete`：确认授课完成
11. `POST /app/booking/review`：授课后评价

## 3. Admin 端接口（已实现）
1. `GET /admin/dashboard/summary`：运营汇总看板
2. `GET /admin/ops/booking/list`：预约列表
3. `POST /admin/ops/booking/complete`：管理员标记授课完成
4. `GET /admin/ops/order/list`：订单列表
5. `POST /admin/ops/booking/close-timeout`：关闭超时未支付预约

## 4. 视频教学接口（已实现）
1. `POST /admin/video/upload`：上传视频到 MinIO（返回 `objectPath` + `previewUrl`）
2. `POST /admin/video`：新增视频素材元数据
3. `PUT /admin/video/{id}`：更新视频素材元数据
4. `PUT /admin/video/{id}/status`：启用/停用素材
5. `GET /admin/video/list`：视频素材列表（支持状态/关键字）
6. `PUT /admin/video/bind-plan-item`：训练计划项绑定视频
7. `PUT /admin/video/unbind-plan-item/{planItemId}`：训练计划项解绑视频

## 5. 端到端测试顺序（你现在可直接照这个测）
1. 启动中间件与服务  
   MySQL: `localhost:3306`，Redis: `localhost:6379`，MinIO: `localhost:9000`  
   admin: `8080`，app: `8081`
2. 获取用户 Token（用于 app 端）
3. Admin 上传教学视频
4. Admin 新增视频素材元数据
5. Admin 将视频绑定到训练计划项
6. App 查询训练计划详情，确认 `items[].video.playUrl` 存在且可播放

## 6. 示例请求
### 6.1 用户登录（获取 app token）
```bash
curl -X POST "http://localhost:8081/app/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"13800000000\",\"password\":\"123456\"}"
```

### 6.2 Admin 上传视频到 MinIO
```bash
curl -X POST "http://localhost:8080/admin/video/upload" \
  -H "Authorization: Bearer <adminToken>" \
  -F "file=@D:/videos/squat.mp4"
```

返回示例（关键字段）：
```json
{
  "code": 200,
  "data": {
    "objectPath": "videos/upload/20260225/abc123.mp4",
    "previewUrl": "http://localhost:9000/lease/videos/upload/20260225/abc123.mp4?...",
    "size": 1024000,
    "contentType": "video/mp4"
  }
}
```

### 6.3 Admin 新增视频素材
```bash
curl -X POST "http://localhost:8080/admin/video" \
  -H "Authorization: Bearer <adminToken>" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\":\"深蹲教学-初级\",
    \"sourceSite\":\"pexels\",
    \"sourceUrl\":\"https://www.pexels.com/video/xxxx/\",
    \"licenseType\":\"Pexels License\",
    \"attributionRequired\":0,
    \"authorName\":\"Pexels Author\",
    \"durationSec\":45,
    \"tags\":\"squat,legs,beginner\",
    \"minioPath\":\"videos/upload/20260225/abc123.mp4\",
    \"status\":1
  }"
```

### 6.4 绑定计划项与视频
```bash
curl -X PUT "http://localhost:8080/admin/video/bind-plan-item" \
  -H "Authorization: Bearer <adminToken>" \
  -H "Content-Type: application/json" \
  -d "{\"planItemId\":1,\"videoId\":3}"
```

### 6.5 App 查询计划详情验证视频播放地址
```bash
curl -X GET "http://localhost:8081/app/plan/1" \
  -H "Authorization: Bearer <appToken>"
```

检查响应：
- `data.items[*].video.videoId`
- `data.items[*].video.playUrl`

