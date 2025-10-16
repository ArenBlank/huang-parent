# Knife4j 完整接口测试指南

## 📋 目录
- [服务端口与访问地址](#服务端口与访问地址)
- [App端接口测试](#app端接口测试)
- [Admin端接口测试](#admin端接口测试)
- [完整业务流程测试](#完整业务流程测试)

---

## 服务端口与访问地址

- **管理端 (Admin)**: http://localhost:8080/doc.html
- **用户端 (App)**: http://localhost:8081/doc.html

---

## 🚀 App端接口测试 (端口: 8081)

### 模块01：用户认证

#### 1.1 发送短信验证码
- **接口**: `POST /app/auth/sms-code/send`
- **分组**: 用户认证
- **请求示例**:
```json
{
  "phone": "13800138001",
  "type": "register"
}
```
- **验证码类型**:
  - `register` - 注册
  - `login` - 登录
  - `reset_password` - 重置密码
  - `cancel_account` - 注销账号
- **【开发模式】**: 验证码功能已暂停，统一返回固定验证码 `123456`

#### 1.2 用户注册
- **接口**: `POST /app/auth/register`
- **前置条件**: 先发送注册验证码
- **【开发模式】**: 验证码统一使用 `123456`
- **请求示例**:
```json
{
  "username": "testuser001",
  "password": "123456",
  "confirmPassword": "123456",
  "phone": "13800138001",
  "smsCode": "123456",
  "nickname": "测试用户"
}
```

#### 1.3 密码登录
- **接口**: `POST /app/auth/login`
- **请求示例**:
```json
{
  "account": "13800138001",
  "loginType": "password",
  "password": "123456",
  "deviceId": "device_123456",
  "deviceType": "web"
}
```

#### 1.4 短信验证码登录
**第一步**: 发送登录验证码
```json
{
  "phone": "13800138001",
  "type": "login"
}
```
- **【开发模式】**: 验证码统一为 `123456`

**第二步**: 验证码登录
```json
{
  "account": "13800138001",
  "loginType": "sms",
  "smsCode": "123456",
  "deviceId": "device_123456",
  "deviceType": "web"
}
```

#### 1.5 忘记密码
**第一步**: 发送重置密码验证码
```json
{
  "phone": "13800138001",
  "type": "reset_password"
}
```
- **【开发模式】**: 验证码统一为 `123456`

**第二步**: 重置密码
- **接口**: `POST /app/auth/forget-password`
```json
{
  "phone": "13800138001",
  "smsCode": "123456",
  "newPassword": "newpass123",
  "confirmPassword": "newpass123"
}
```

#### 1.6 退出登录
- **接口**: `POST /app/auth/logout`
- **认证**: 需要Bearer Token

---

### 模块02：个人信息管理 🔐

> **重要**: 以下接口需要JWT认证，请先登录获取token

#### 设置JWT认证:
1. 点击Knife4j页面右上角 **🔒 Authorize** 按钮
2. 在弹出框中输入登录获取的token（不需要加Bearer前缀）
3. 点击 **Authorize** 确认

#### 2.1 获取个人信息
- **接口**: `GET /app/profile/info`
- **认证**: Bearer Token
- **返回**: 用户详细信息、资料完成度等

#### 2.2 修改个人信息
- **接口**: `PUT /app/profile/info`
- **认证**: Bearer Token
- **请求示例**:
```json
{
  "nickname": "新昵称",
  "email": "test@example.com",
  "gender": 1,
  "birthDate": "1990-01-01",
  "bio": "健身爱好者",
  "height": 175.5,
  "weight": 70.0,
  "occupation": "程序员",
  "address": "北京市朝阳区"
}
```

#### 2.3 上传头像
- **接口**: `POST /app/profile/avatar/upload`
- **认证**: Bearer Token
- **参数**: `file` (图片文件，最大5MB)

#### 2.4 修改密码
- **接口**: `PUT /app/profile/password`
- **认证**: Bearer Token
- **请求示例**:
```json
{
  "oldPassword": "123456",
  "newPassword": "654321",
  "confirmPassword": "654321"
}
```

#### 2.5 申请账号注销
**第一步**: 发送注销验证码
```json
{
  "phone": "13800138001",
  "type": "cancel_account"
}
```
- **【开发模式】**: 验证码统一为 `123456`

**第二步**: 提交注销申请
- **接口**: `POST /app/profile/cancel`
- **认证**: Bearer Token
- **请求示例**:
```json
{
  "cancelType": "temporary",
  "reason": "暂时不用了",
  "password": "123456",
  "smsCode": "123456"
}
```
- **注销类型**:
  - `temporary` - 临时注销(7天后生效)
  - `permanent` - 永久注销(30天后生效)

#### 2.6 撤销注销申请
- **接口**: `POST /app/profile/cancel/{cancelId}/revoke`
- **认证**: Bearer Token

#### 2.7 获取账号统计信息
- **接口**: `GET /app/profile/statistics`
- **认证**: Bearer Token

---

### 模块03：角色管理 🔐

#### 3.1 获取当前用户角色列表
- **接口**: `GET /app/role/list`
- **认证**: Bearer Token

#### 3.2 获取角色详情
- **接口**: `GET /app/role/{roleId}`
- **认证**: Bearer Token

---

### 模块04：教练认证申请 🔐

#### 4.1 检查申请资格
- **接口**: `GET /app/coach/certification/check-eligibility`
- **认证**: Bearer Token
- **返回**: 是否可以申请、原因等

#### 4.2 提交教练认证申请
- **接口**: `POST /app/coach/certification/apply`
- **认证**: Bearer Token
- **请求示例**:
```json
{
  "realName": "张三",
  "idCard": "110101199001011234",
  "phone": "13800138001",
  "email": "coach@example.com",
  "specialties": "瑜伽、普拉提",
  "experienceYears": 5,
  "introduction": "资深健身教练",
  "certificates": [
    {
      "name": "国家健身教练资格证",
      "issueOrg": "国家体育总局",
      "issueDate": "2018-06-01",
      "imageUrl": "/uploads/cert1.jpg"
    }
  ],
  "education": [
    {
      "school": "北京体育大学",
      "major": "运动训练",
      "degree": "本科",
      "startDate": "2010-09-01",
      "endDate": "2014-06-30"
    }
  ],
  "workExperience": [
    {
      "company": "XX健身中心",
      "position": "高级教练",
      "startDate": "2014-07-01",
      "endDate": "2023-12-31",
      "description": "负责私教课程"
    }
  ]
}
```

#### 4.3 获取当前申请状态
- **接口**: `GET /app/coach/certification/current`
- **认证**: Bearer Token

#### 4.4 获取申请历史记录
- **接口**: `GET /app/coach/certification/history`
- **认证**: Bearer Token

#### 4.5 取消申请
- **接口**: `DELETE /app/coach/certification/{applicationId}/cancel`
- **认证**: Bearer Token
- **参数**: `cancelReason` (可选)

---

### 模块05：教练离职管理 🔒

> **重要**: 需要先成为教练才能使用这些接口

#### 5.1 提交教练离职申请
- **接口**: `POST /app/coach/resignation/apply`
- **分组**: 05-教练离职管理
- **认证**: Bearer Token
- **请求方式**: application/json
- **请求示例**:
```json
{
  "resignationDate": "2025-02-15",
  "reason": "个人原因，需要照顾家庭",
  "handoverPlan": "将所有在约课程交接给李教练，预计3天完成交接"
}
```
- **参数说明**:
  - `resignationDate` (必填): 预计离职日期，格式: yyyy-MM-dd，必须是未来日期
  - `reason` (必填): 离职原因，最多500字
  - `handoverPlan` (可选): 工作交接计划，最多1000字

#### 5.2 获取当前离职申请状态
- **接口**: `GET /app/coach/resignation/current-status`
- **分组**: 05-教练离职管理
- **认证**: Bearer Token
- **说明**: 查询当前教练是否有待审核的离职申请

#### 5.3 获取离职申请历史
- **接口**: `GET /app/coach/resignation/history`
- **分组**: 05-教练离职管理
- **认证**: Bearer Token
- **说明**: 查询教练的所有离职申请记录，按申请时间降序排列

#### 5.4 获取离职申请详情
- **接口**: `GET /app/coach/resignation/{applicationId}`
- **分组**: 05-教练离职管理
- **认证**: Bearer Token

#### 5.5 撤销离职申请
- **接口**: `DELETE /app/coach/resignation/{applicationId}/cancel`
- **分组**: 05-教练离职管理
- **认证**: Bearer Token
- **说明**: 只有待审核(pending)状态的申请才能撤销

---

### 模块06：教练咨询 🔐

> **重要**: 用户端接口，普通用户可预约教练咨询

#### 6.1 预约教练咨询
- **接口**: `POST /app/consultation/book`
- **分组**: 06-教练咨询
- **认证**: Bearer Token
- **请求示例**:
```json
{
  "coachId": 1,
  "consultationType": "online",
  "consultationDate": "2025-02-01 10:00:00",
  "duration": 60,
  "topic": "健身计划咨询",
  "content": "想了解如何制定适合自己的健身计划"
}
```
- **参数说明**:
  - `coachId` (必填): 教练ID
  - `consultationType` (必填): 咨询类型 - `online`在线 / `offline`线下
  - `consultationDate` (必填): 咨询时间，格式: yyyy-MM-dd HH:mm:ss，必须是未来时间
  - `duration` (必填): 咨询时长(分钟)，最少30分钟
  - `topic` (必填): 咨询主题，最多200字
  - `content` (可选): 咨询内容，最多1000字

#### 6.2 查看我的咨询记录
- **接口**: `GET /app/consultation/my-list`
- **分组**: 06-教练咨询
- **认证**: Bearer Token
- **查询参数**:
  - `status` (可选): 状态筛选 - `scheduled`已预约 / `completed`已完成 / `cancelled`已取消
- **请求示例**:
```
GET /app/consultation/my-list?status=scheduled
```

#### 6.3 获取咨询详情
- **接口**: `GET /app/consultation/{id}`
- **分组**: 06-教练咨询
- **认证**: Bearer Token
- **请求示例**:
```
GET /app/consultation/1
```

#### 6.4 取消咨询
- **接口**: `DELETE /app/consultation/{id}/cancel`
- **分组**: 06-教练咨询
- **认证**: Bearer Token
- **说明**: 只有未开始的咨询(scheduled状态)可以取消
- **请求示例**:
```
DELETE /app/consultation/1/cancel
```

---

### 模块07：教练日程管理 🔐

> **重要**: 需要先成为教练才能使用这些接口

#### 7.1 查看我的工作时间安排
- **接口**: `GET /app/coach/schedule/availability/list`
- **分组**: 07-教练日程管理
- **认证**: Bearer Token
- **说明**: 查询当前教练的一周工作时间安排（周一到周日）

#### 7.2 设置我的工作时间
- **接口**: `POST /app/coach/schedule/availability/set`
- **分组**: 07-教练日程管理
- **认证**: Bearer Token
- **请求示例**:
```json
{
  "dayOfWeek": 1,
  "startTime": "09:00:00",
  "endTime": "18:00:00",
  "isAvailable": 1
}
```
- **参数说明**:
  - `dayOfWeek`: 星期几(1-7，1为周一，7为周日)
  - `startTime`: 开始时间(格式：HH:mm:ss)
  - `endTime`: 结束时间(格式：HH:mm:ss)
  - `isAvailable`: 是否可用(1可用，0不可用)

#### 7.3 删除工作时间设置
- **接口**: `DELETE /app/coach/schedule/availability/{id}`
- **分组**: 07-教练日程管理
- **认证**: Bearer Token

#### 7.4 申请日程调整
- **接口**: `POST /app/coach/schedule/change/apply`
- **分组**: 07-教练日程管理
- **认证**: Bearer Token
- **请求示例**:
```json
{
  "changeType": "leave",
  "originalDate": "2025-02-15",
  "originalStartTime": "09:00:00",
  "originalEndTime": "12:00:00",
  "reason": "有急事需要请假"
}
```
- **变更类型说明**:
  - `leave`: 请假
  - `overtime`: 加班
  - `adjust`: 调整

#### 7.5 查看我的日程调整申请
- **接口**: `GET /app/coach/schedule/change/page`
- **分组**: 07-教练日程管理
- **认证**: Bearer Token
- **查询参数**:
  - `current`: 当前页码(默认1)
  - `size`: 每页大小(默认10)
  - `status`: 状态筛选(pending/approved/rejected)
  - `changeType`: 变更类型(leave/overtime/adjust)
  - `startDate`: 开始日期
  - `endDate`: 结束日期

#### 7.6 查看日程调整申请详情
- **接口**: `GET /app/coach/schedule/change/{id}`
- **分组**: 07-教练日程管理
- **认证**: Bearer Token

#### 7.7 取消日程调整申请
- **接口**: `DELETE /app/coach/schedule/change/{id}`
- **分组**: 07-教练日程管理
- **认证**: Bearer Token
- **说明**: 只能取消待审核状态的申请

---

## 🛠️ Admin端接口测试 (端口: 8080)

### 模块01：用户权限管理

#### 1.1 用户账号注销申请管理

##### 获取注销申请列表
- **接口**: `GET /admin/user-account-cancel/list`
- **分组**: 用户权限管理
- **参数**:
  - `pageNum`: 页码 (默认1)
  - `pageSize`: 每页数量 (默认10)
  - `status`: 状态筛选 (pending/approved/rejected/cancelled)
  - `username`: 用户名搜索
  - `phone`: 手机号搜索

##### 获取注销申请详情
- **接口**: `GET /admin/user-account-cancel/{id}`
- **参数**: 申请ID

##### 审核注销申请
- **接口**: `PUT /admin/user-account-cancel/{id}/review`
- **参数**: 申请ID
- **请求示例**:
```json
{
  "reviewStatus": "approved",
  "reviewRemark": "审核通过，同意注销申请"
}
```

##### 获取待审核数量
- **接口**: `GET /admin/user-account-cancel/pending-count`
- **返回**: 待审核的注销申请数量

---

### 模块02：教练管理

#### 2.1 教练认证申请管理

##### 分页查询教练认证申请列表
- **接口**: `GET /admin/coach/certification/list`
- **分组**: 教练管理
- **参数**:
  - `page`: 页码 (默认1)
  - `size`: 每页大小 (默认10)
  - `realName`: 真实姓名搜索
  - `phone`: 联系电话搜索
  - `status`: 状态筛选 (pending/approved/rejected)
  - `specialties`: 专长领域搜索
  - `startTime`: 申请开始时间 (格式: 2025-01-01 00:00:00)
  - `endTime`: 申请结束时间 (格式: 2025-12-31 23:59:59)
  - `sortOrder`: 排序方式 (asc/desc，默认desc)

##### 获取教练认证申请详情
- **接口**: `GET /admin/coach/certification/detail/{id}`
- **参数**: 申请ID

##### 审核教练认证申请
- **接口**: `PUT /admin/coach/certification/review`
- **请求示例**:
```json
{
  "id": 1,
  "status": "approved",
  "reviewRemark": "申请材料齐全，符合教练认证要求",
  "certificationNo": "COACH202501240001"
}
```

##### 快捷批准教练认证申请
- **接口**: `PUT /admin/coach/certification/{id}/approve`
- **参数**:
  - `id`: 申请ID（路径参数）
  - `reviewRemark`: 审核备注（可选）

##### 快捷拒绝教练认证申请
- **接口**: `PUT /admin/coach/certification/{id}/reject`
- **参数**:
  - `id`: 申请ID（路径参数）
  - `reviewRemark`: 拒绝原因（必填）

#### 2.2 教练离职申请管理

##### 获取教练离职申请列表
- **接口**: `GET /admin/coach/resignation/list`
- **分组**: 教练管理
- **参数**:
  - `current`: 当前页码（默认1）
  - `size`: 每页大小（默认10）
  - `coachId`: 教练ID
  - `coachName`: 教练姓名搜索
  - `status`: 状态筛选 (pending/approved/rejected/cancelled)
  - `applyStartDate`: 申请开始时间
  - `applyEndDate`: 申请结束时间
  - `resignationStartDate`: 预计离职开始日期
  - `resignationEndDate`: 预计离职结束日期

##### 查看教练离职申请详情
- **接口**: `GET /admin/coach/resignation/{id}`
- **参数**: 申请ID

##### 批准教练离职申请
- **接口**: `POST /admin/coach/resignation/approve/{id}`
- **参数**: 申请ID（路径参数）
- **请求示例**:
```json
{
  "reviewRemark": "同意离职申请，感谢您的贡献",
  "actualLeaveDate": "2025-02-01"
}
```

##### 拒绝教练离职申请
- **接口**: `POST /admin/coach/resignation/reject/{id}`
- **参数**: 申请ID（路径参数）
- **请求示例**:
```json
{
  "reviewRemark": "当前项目需要您，请再考虑一下"
}
```

##### 取消教练离职申请
- **接口**: `DELETE /admin/coach/resignation/cancel/{id}`
- **参数**:
  - `id`: 申请ID（路径参数）
  - `cancelReason`: 取消原因（可选）

#### 2.3 教练咨询记录管理

##### 分页查询咨询记录
- **接口**: `GET /admin/coach-consultation/page`
- **分组**: 教练管理
- **参数**:
  - `current`: 当前页码（默认1）
  - `size`: 每页大小（默认10）
  - `coachId`: 教练ID
  - `userId`: 用户ID
  - `consultationType`: 咨询类型(online/offline)
  - `status`: 状态(scheduled/completed/cancelled)
  - `coachName`: 教练姓名
  - `userNickname`: 用户昵称
  - `startDate`: 开始日期
  - `endDate`: 结束日期
- **请求示例**:
```
GET /admin/coach-consultation/page?current=1&size=10&status=scheduled
```

##### 获取咨询详情
- **接口**: `GET /admin/coach-consultation/{id}`
- **分组**: 教练管理
- **参数**: 咨询ID

##### 更新咨询状态
- **接口**: `PUT /admin/coach-consultation/{id}/status`
- **分组**: 教练管理
- **参数**:
  - `id`: 咨询ID
  - `status`: 状态值(scheduled/completed/cancelled)
- **请求示例**:
```
PUT /admin/coach-consultation/1/status?status=completed
```

##### 添加教练建议
- **接口**: `PUT /admin/coach-consultation/{id}/advice`
- **分组**: 教练管理
- **参数**:
  - `id`: 咨询ID
  - `coachAdvice`: 教练建议
- **请求示例**:
```
PUT /admin/coach-consultation/1/advice?coachAdvice=建议增加有氧训练
```

##### 获取教练咨询统计
- **接口**: `GET /admin/coach-consultation/coach/{coachId}/stats`
- **分组**: 教练管理
- **参数**: 教练ID

##### 获取用户咨询历史
- **接口**: `GET /admin/coach-consultation/user/{userId}/history`
- **分组**: 教练管理
- **参数**: 用户ID

##### 获取咨询统计数据
- **接口**: `GET /admin/coach-consultation/statistics`
- **分组**: 教练管理

##### 批量删除咨询记录
- **接口**: `DELETE /admin/coach-consultation/batch`
- **分组**: 教练管理
- **请求示例**:
```json
[1, 2, 3, 4, 5]
```

#### 2.4 教练日程审核管理

##### 分页查询日程变更申请
- **接口**: `GET /admin/coach/schedule/change/page`
- **分组**: 教练管理
- **参数**:
  - `current`: 当前页码(默认1)
  - `size`: 每页大小(默认10)
  - `coachId`: 教练ID筛选
  - `coachName`: 教练姓名模糊查询
  - `status`: 状态筛选(pending/approved/rejected)
  - `changeType`: 变更类型(leave/overtime/adjust)
  - `applyStartDate`: 申请开始日期
  - `applyEndDate`: 申请结束日期
  - `originalStartDate`: 原日期开始范围
  - `originalEndDate`: 原日期结束范围
- **请求示例**:
```
GET /admin/coach/schedule/change/page?current=1&size=10&status=pending
```

##### 查询日程变更申请详情
- **接口**: `GET /admin/coach/schedule/change/{id}`
- **分组**: 教练管理
- **参数**: 申请ID

##### 审核日程变更申请
- **接口**: `POST /admin/coach/schedule/change/review`
- **分组**: 教练管理
- **请求示例**:
```json
{
  "id": 1,
  "status": "approved",
  "reviewRemark": "同意请假申请"
}
```
- **参数说明**:
  - `id`: 申请ID
  - `status`: 审核结果(approved已批准/rejected已拒绝)
  - `reviewRemark`: 审核备注

---

### 模块03：统计管理 ⭐ NEW

#### 3.1 获取账号统计信息
- **接口**: `GET /admin/statistics/account`
- **分组**: 系统管理
- **说明**: 获取平台账号统计信息，包括用户数、会员数、教练数等
- **响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalUsers": 12,
    "activeUsers": 12,
    "todayNewUsers": 0,
    "todayLoginUsers": 5,
    "todayLogins": 8,
    "weekNewUsers": 2,
    "monthNewUsers": 3,
    "normalMembers": 4,
    "vipMembers": 3,
    "coachCount": 5
  }
}
```

#### 3.2 手动执行课程统计任务
- **接口**: `POST /admin/statistics/task/course`
- **分组**: 系统管理
- **说明**: 手动触发课程统计任务，统计前一天的课程数据
- **响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "课程统计任务执行成功"
}
```

#### 3.3 手动执行教练统计任务
- **接口**: `POST /admin/statistics/task/coach`
- **分组**: 系统管理
- **说明**: 手动触发教练工作统计任务，统计前一天的教练工作数据
- **响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "教练统计任务执行成功"
}
```

#### 3.4 手动执行课程状态检查任务
- **接口**: `POST /admin/statistics/task/course-status`
- **分组**: 系统管理
- **说明**: 手动触发课程状态检查，自动将已结束的课程标记为"已结束"状态

#### 3.5 手动执行临时文件清理任务
- **接口**: `POST /admin/statistics/task/clean-temp-files`
- **分组**: 系统管理
- **说明**: 手动触发过期临时文件清理任务

---

### 模块04：权限管理 ⭐ NEW

#### 4.1 根据角色ID查询权限列表
- **接口**: `GET /admin/permission/role/{roleId}`
- **分组**: 系统管理
- **参数**: `roleId` - 角色ID
- **说明**: 查询指定角色拥有的所有权限
- **请求示例**:
```
GET /admin/permission/role/1
```
- **响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "permissionName": "用户管理",
      "permissionCode": "user_management",
      "permissionType": "menu",
      "parentId": 0,
      "resourcePath": "/admin/user",
      "resourceMethod": null,
      "sortOrder": 1,
      "description": "用户信息管理模块",
      "status": 1
    }
  ]
}
```

#### 4.2 根据用户ID查询权限列表
- **接口**: `GET /admin/permission/user/{userId}`
- **分组**: 系统管理
- **参数**: `userId` - 用户ID
- **说明**: 查询指定用户拥有的所有权限（通过用户角色关联）
- **请求示例**:
```
GET /admin/permission/user/1
```

#### 4.3 检查用户是否有指定权限
- **接口**: `GET /admin/permission/check`
- **分组**: 系统管理
- **参数**:
  - `userId` - 用户ID
  - `permissionCode` - 权限编码
- **说明**: 检查指定用户是否拥有某个权限，返回true/false
- **请求示例**:
```
GET /admin/permission/check?userId=1&permissionCode=user:add
```
- **响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

---

## 📝 完整业务流程测试

### 流程一：新用户注册到教练认证

#### 步骤1：用户注册
1. **发送注册验证码**: `POST /app/auth/sms-code/send`
```json
{
  "phone": "13900139001",
  "type": "register"
}
```

2. **用户注册**: `POST /app/auth/register`
```json
{
  "username": "newcoach001",
  "password": "123456",
  "confirmPassword": "123456",
  "phone": "13900139001",
  "smsCode": "123456",
  "nickname": "新教练"
}
```

3. **登录获取Token**: `POST /app/auth/login`
```json
{
  "account": "13900139001",
  "loginType": "password",
  "password": "123456",
  "deviceId": "device_001",
  "deviceType": "web"
}
```

#### 步骤2：完善个人信息
4. **修改个人信息**: `PUT /app/profile/info`
```json
{
  "nickname": "健身达人",
  "email": "newcoach@example.com",
  "gender": 1,
  "birthDate": "1990-05-15",
  "bio": "热爱健身的教练",
  "height": 178.0,
  "weight": 75.0
}
```

#### 步骤3：申请教练认证
5. **检查申请资格**: `GET /app/coach/certification/check-eligibility`

6. **提交教练认证申请**: `POST /app/coach/certification/apply`
```json
{
  "realName": "王教练",
  "idCard": "110101199005151234",
  "phone": "13900139001",
  "email": "newcoach@example.com",
  "specialties": "力量训练、功能性训练",
  "experienceYears": 3,
  "introduction": "3年健身教练经验，擅长力量训练",
  "certificates": [
    {
      "name": "国家健身教练资格证",
      "issueOrg": "国家体育总局",
      "issueDate": "2020-06-01",
      "imageUrl": "/uploads/cert.jpg"
    }
  ],
  "education": [],
  "workExperience": []
}
```

#### 步骤4：Admin端审核
7. **Admin登录**: `POST /admin/auth/login`

8. **查看待审核申请**: `GET /admin/coach/certification/list?status=pending`

9. **批准认证申请**: `PUT /admin/coach/certification/1/approve?reviewRemark=审核通过`

#### 步骤5：教练设置工作日程
10. **设置周一工作时间**: `POST /app/coach/schedule/availability/set`
```json
{
  "dayOfWeek": 1,
  "startTime": "09:00:00",
  "endTime": "18:00:00",
  "isAvailable": 1
}
```

11. **设置周二工作时间**: `POST /app/coach/schedule/availability/set`
```json
{
  "dayOfWeek": 2,
  "startTime": "09:00:00",
  "endTime": "18:00:00",
  "isAvailable": 1
}
```

---

### 流程二：用户预约教练咨询完整流程

#### 步骤1：用户登录
1. **用户登录**: `POST /app/auth/login`
```json
{
  "account": "testuser001",
  "loginType": "password",
  "password": "123456",
  "deviceId": "device_123",
  "deviceType": "web"
}
```

#### 步骤2：查看教练并预约
2. **预约教练咨询**: `POST /app/consultation/book`
```json
{
  "coachId": 1,
  "consultationType": "online",
  "consultationDate": "2025-02-20 14:00:00",
  "duration": 60,
  "topic": "减脂训练计划咨询",
  "content": "想咨询如何科学减脂，制定合适的训练计划"
}
```

#### 步骤3：查看预约记录
3. **查看我的咨询记录**: `GET /app/consultation/my-list?status=scheduled`

4. **查看咨询详情**: `GET /app/consultation/1`

#### 步骤4：Admin端管理咨询
5. **Admin查看所有咨询**: `GET /admin/coach-consultation/page?current=1&size=10`

6. **查看咨询详情**: `GET /admin/coach-consultation/1`

7. **添加教练建议**: `PUT /admin/coach-consultation/1/advice?coachAdvice=建议每周3次力量训练+2次有氧运动`

8. **更新咨询状态为已完成**: `PUT /admin/coach-consultation/1/status?status=completed`

#### 步骤5：获取统计数据
9. **获取教练咨询统计**: `GET /admin/coach-consultation/coach/1/stats`

10. **获取用户咨询历史**: `GET /admin/coach-consultation/user/1/history`

11. **获取整体咨询统计**: `GET /admin/coach-consultation/statistics`

---

### 流程三：教练日程变更审核完整流程

#### 步骤1：教练申请日程变更
1. **教练登录**: `POST /app/auth/login`
```json
{
  "account": "coach001",
  "loginType": "password",
  "password": "123456",
  "deviceId": "device_coach",
  "deviceType": "web"
}
```

2. **提交请假申请**: `POST /app/coach/schedule/change/apply`
```json
{
  "changeType": "leave",
  "originalDate": "2025-02-18",
  "originalStartTime": "09:00:00",
  "originalEndTime": "12:00:00",
  "reason": "家中有事需要请假半天"
}
```

3. **查看申请记录**: `GET /app/coach/schedule/change/page?status=pending`

4. **查看申请详情**: `GET /app/coach/schedule/change/1`

#### 步骤2：Admin端审核
5. **Admin查看待审核申请**: `GET /admin/coach/schedule/change/page?status=pending`

6. **查看申请详情**: `GET /admin/coach/schedule/change/1`

7. **批准申请**: `POST /admin/coach/schedule/change/review`
```json
{
  "id": 1,
  "status": "approved",
  "reviewRemark": "同意请假，已安排代课教练"
}
```

#### 步骤3：教练查看审核结果
8. **查看申请状态**: `GET /app/coach/schedule/change/1`

---

### 流程四：教练离职完整流程

#### 步骤1：教练提交离职申请
1. **教练登录**: `POST /app/auth/login`

2. **提交离职申请**: `POST /app/coach/resignation/apply`
```json
{
  "resignationDate": "2025-03-15",
  "reason": "个人发展原因",
  "handoverPlan": "将所有会员课程交接给张教练"
}
```

3. **查看当前申请状态**: `GET /app/coach/resignation/current-status`

4. **查看申请历史**: `GET /app/coach/resignation/history`

#### 步骤2：Admin端审核
5. **Admin查看离职申请列表**: `GET /admin/coach/resignation/list?status=pending`

6. **查看申请详情**: `GET /admin/coach/resignation/1`

7. **批准离职申请**: `POST /admin/coach/resignation/approve/1`
```json
{
  "reviewRemark": "同意离职申请，感谢您的付出",
  "actualLeaveDate": "2025-03-10"
}
```

#### 步骤3：教练查看审核结果
8. **查看申请详情**: `GET /app/coach/resignation/1`

---

### 流程五：统计管理与权限验证流程 ⭐ NEW

#### 步骤1：获取平台统计数据
1. **Admin登录**: 登录Admin端获取Token

2. **查看账号统计**: `GET /admin/statistics/account`
```
GET http://localhost:8080/admin/statistics/account
```
- **预期返回**: 总用户数、活跃用户数、会员数、教练数等统计信息

3. **查看用户登录情况**: 从统计信息中获取今日登录数据
```json
{
  "todayLoginUsers": 5,
  "todayLogins": 8
}
```

#### 步骤2：手动触发定时任务
4. **执行课程统计**: `POST /admin/statistics/task/course`
```
POST http://localhost:8080/admin/statistics/task/course
```
- **说明**: 统计昨天的课程数据(新增报名、完成课程、签到等)

5. **执行教练统计**: `POST /admin/statistics/task/coach`
```
POST http://localhost:8080/admin/statistics/task/coach
```
- **说明**: 统计昨天的教练工作数据(授课次数、收入、评分等)

6. **检查课程状态**: `POST /admin/statistics/task/course-status`
```
POST http://localhost:8080/admin/statistics/task/course-status
```
- **说明**: 自动将已结束的课程更新为"已结束"状态

7. **清理临时文件**: `POST /admin/statistics/task/clean-temp-files`
```
POST http://localhost:8080/admin/statistics/task/clean-temp-files
```
- **说明**: 清理过期的临时文件

#### 步骤3：权限管理验证
8. **查询管理员角色权限**: `GET /admin/permission/role/1`
```
GET http://localhost:8080/admin/permission/role/1
```
- **预期返回**: 管理员角色拥有的所有权限列表

9. **查询指定用户权限**: `GET /admin/permission/user/1`
```
GET http://localhost:8080/admin/permission/user/1
```
- **预期返回**: 用户ID为1的所有权限(通过角色关联)

10. **检查用户是否有添加用户权限**:
```
GET http://localhost:8080/admin/permission/check?userId=1&permissionCode=user:add
```
- **预期返回**: `true` 或 `false`

11. **检查普通用户权限**:
```
GET http://localhost:8080/admin/permission/check?userId=5&permissionCode=user:add
```
- **预期返回**: `false` (普通用户没有用户管理权限)

#### 步骤4：验证定时任务自动执行
- **课程统计**: 每天凌晨1点自动执行
- **教练统计**: 每天凌晨2点自动执行
- **课程状态检查**: 每小时整点自动执行
- **临时文件清理**: 每天凌晨3点自动执行

**查看日志验证**:
```
=== 开始执行课程统计定时任务 ===
课程统计任务完成: 日期=2025-10-14, 新增报名=5, 完成课程=3
=== 课程统计定时任务执行成功 ===
```

---

## ⚠️ 注意事项

1. **JWT认证**:
   - App端所有需要认证的接口都需要先登录获取token
   - 在Knife4j界面右上角点击🔒设置认证
   - Token有效期为24小时，过期需要重新登录

2. **验证码**:
   - **【开发模式】**: 验证码功能已暂停，所有验证码统一为 `123456`
   - 验证码有效期为5分钟
   - 同一手机号60秒内只能发送一次

3. **教练认证流程**:
   - 用户必须先注册并完善个人信息
   - 提交认证申请后需要Admin审核
   - 审核通过后系统自动创建教练账号并分配教练角色
   - 认证被拒绝后可以重新申请

4. **教练咨询功能**:
   - 咨询时间必须是未来时间
   - 咨询时长最少30分钟
   - 只有scheduled状态的咨询可以取消
   - 咨询完成后可以添加教练建议

5. **教练日程管理**:
   - 只有认证教练才能设置工作日程
   - dayOfWeek: 1-7 (周一到周日)
   - 时间格式: HH:mm:ss
   - 只有待审核状态的日程变更可以取消

6. **教练离职申请**:
   - 只有教练身份的用户才能提交离职申请
   - 离职日期必须是未来日期
   - 只有待审核状态的申请可以撤销
   - 同一时间只能有一个待审核的离职申请
   - Admin批准离职后，教练状态将更新为离职，教练角色将被移除

7. **统计管理** ⭐ NEW:
   - 账号统计接口返回实时数据，直接从数据库查询
   - 定时任务支持手动触发，方便测试和调试
   - 定时任务有完善的日志记录，执行情况可追踪
   - 课程统计和教练统计会检查是否已统计，避免重复
   - 临时文件清理只更新状态，实际删除需要接入MinIO

8. **权限管理** ⭐ NEW:
   - 权限管理为模拟实现，提供完整的RBAC功能
   - 管理员(userId=1)默认拥有所有权限
   - 其他用户权限通过角色关联查询
   - 支持菜单权限、按钮权限、API权限三级权限
   - 资源路径支持通配符匹配

9. **用户登录日志**:
   - 每次用户登录(成功/失败)都会自动记录
   - 记录登录时间、IP、设备等信息
   - 支持统计今日登录用户数和登录次数
   - Admin端和App端都支持日志记录

10. **请求路径对应**:
   - App端认证接口: `/app/auth/**` (模块01-用户认证)
   - App端个人信息: `/app/profile/**` (模块02-个人信息管理)
   - App端角色管理: `/app/role/**` (模块03-角色管理)
   - App端教练认证: `/app/coach/certification/**` (模块04-教练认证申请)
   - App端教练离职: `/app/coach/resignation/**` (模块05-教练离职管理)
   - App端教练咨询: `/app/consultation/**` (模块06-教练咨询)
   - App端教练日程: `/app/coach/schedule/**` (模块07-教练日程管理)
   - Admin端用户管理: `/admin/user-account-cancel/**` (模块01-用户权限管理)
   - Admin端教练认证: `/admin/coach/certification/**` (模块02-教练管理)
   - Admin端教练离职: `/admin/coach/resignation/**` (模块02-教练管理)
   - Admin端教练咨询: `/admin/coach-consultation/**` (模块02-教练管理)
   - Admin端日程审核: `/admin/coach/schedule/**` (模块02-教练管理)
   - Admin端统计管理: `/admin/statistics/**` (模块03-统计管理) ⭐ NEW
   - Admin端权限管理: `/admin/permission/**` (模块04-权限管理) ⭐ NEW

---

## 📚 更新日志

- 2025-10-15:
  - ✅ 新增**模块03：统计管理**功能模块(5个接口)
  - ✅ 新增**模块04：权限管理**功能模块(3个接口)
  - ✅ 实现账号统计信息接口，提供用户、会员、教练等统计数据
  - ✅ 实现定时任务手动触发接口(课程统计、教练统计、课程状态检查、临时文件清理)
  - ✅ 实现RBAC权限管理接口(角色权限查询、用户权限查询、权限检查)
  - ✅ 完成项目双端编译测试，Admin端和App端都编译成功
  - ✅ 更新Swagger配置，新增统计管理和权限管理分组

- 2025-10-14:
  - ✅ 修复教练日程管理功能Token解析问题
  - ✅ 优化Service层使用LoginUserHolder获取用户信息
  - ✅ 优化所有接口描述，改为更用户友好的语言
  - ✅ 统一开发模式验证码说明为"【开发模式】"格式
  - ✅ 更新模块07接口描述：
    - 7.1: "查询教练可用性列表" → "查看我的工作时间安排"
    - 7.2: "设置教练可用性" → "设置我的工作时间"
    - 7.3: "删除教练可用性设置" → "删除工作时间设置"
    - 7.4: "申请日程变更" → "申请日程调整"
    - 7.5: "分页查询日程变更申请" → "查看我的日程调整申请"
    - 7.6: "查询日程变更申请详情" → "查看日程调整申请详情"
    - 7.7: "取消日程变更申请" → "取消日程调整申请"

- 2025-10-13:
  - ✅ 修复教练日程管理功能的启动问题
  - ✅ 完善教练日程管理功能包路径，符合项目架构规范
  - ✅ 移动所有教练日程相关类到正确的包路径
  - ✅ 验证Admin端和App端都能正常启动
  - ✅ 新增**模块06：教练咨询**完整测试文档
  - ✅ 新增**模块07：教练日程管理**完整测试文档
  - ✅ 新增**流程二：用户预约教练咨询完整流程**
  - ✅ 新增**流程三：教练日程变更审核完整流程**
  - ✅ 新增Admin端教练咨询记录管理接口文档(8个接口)
  - ✅ 新增Admin端教练日程审核管理接口文档(3个接口)
  - ✅ 补充所有接口的完整请求参数和响应示例

- 2025-01-25:
  - ✅ 修复Admin端教练离职申请管理SQL错误
  - ✅ 新增App端教练离职申请完整功能
  - ✅ 完善教练认证和离职审核流程
  - ✅ 添加完整测试用例和请求/响应示例

- 2025-01-24:
  - ✅ 修复短信登录功能
  - ✅ 添加用户账号注销申请功能
  - ✅ 新增Admin端账号注销审核管理接口
  - ✅ 更新Knife4j配置

---

## 🎉 总结

本文档提供了健身平台所有核心功能的完整接口测试指南，包括：

- ✨ **7个App端功能模块** - 用户认证、个人信息、角色管理、教练认证、教练离职、教练咨询、教练日程管理
- 🛠️ **6个Admin端管理模块** - 用户权限管理、教练认证审核、教练离职审核、教练咨询管理、教练日程审核、统计管理⭐、权限管理⭐
- 📝 **5个完整业务流程** - 新用户注册到教练认证、用户预约教练咨询、教练日程变更审核、教练离职审核、统计管理与权限验证⭐
- 🔐 **完整的认证和权限控制** - JWT认证、角色权限、状态管理、RBAC权限体系⭐
- 📊 **统计分析功能** - 账号统计、课程统计、教练统计、定时任务、用户登录日志⭐

通过本文档，您可以完整测试健身平台的**用户管理、教练管理、咨询服务、日程管理、统计分析、权限控制**等核心功能模块。