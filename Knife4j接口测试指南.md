# Knife4j 完整接口测试指南

## 📋 目录
- [服务端口与访问地址](#服务端口与访问地址)
- [App端接口测试](#app端接口测试)
- [Admin端接口测试](#admin端接口测试)

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
- **开发模式**: 返回固定验证码 `123456`

#### 1.2 用户注册
- **接口**: `POST /app/auth/register`
- **前置条件**: 先发送注册验证码
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
- **返回示例**:
```json
{
  "code": 200,
  "message": "成功",
  "data": 1
}
```
- **错误场景**:
  - 用户不是教练: "您还不是教练,无法申请离职"
  - 已经离职: "您已经离职,无需重复申请"
  - 有待审核申请: "您有正在审核中的离职申请,请勿重复提交"

#### 5.2 获取当前离职申请状态
- **接口**: `GET /app/coach/resignation/current-status`
- **分组**: 05-教练离职管理
- **认证**: Bearer Token
- **说明**: 查询当前教练是否有待审核的离职申请
- **返回示例** (有待审核申请):
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "coachId": 1,
    "coachName": "张教练",
    "resignationDate": "2025-02-15",
    "reason": "个人原因，需要照顾家庭",
    "handoverPlan": "将所有在约课程交接给李教练",
    "status": "pending",
    "statusDesc": "待审核",
    "applyTime": "2025-01-25 10:30:00",
    "canCancel": true
  }
}
```
- **返回示例** (无待审核申请):
```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

#### 5.3 获取离职申请历史
- **接口**: `GET /app/coach/resignation/history`
- **分组**: 05-教练离职管理
- **认证**: Bearer Token
- **说明**: 查询教练的所有离职申请记录，按申请时间降序排列
- **返回示例**:
```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": 2,
      "coachId": 1,
      "coachName": "张教练",
      "resignationDate": "2025-02-15",
      "reason": "个人原因",
      "status": "pending",
      "statusDesc": "待审核",
      "applyTime": "2025-01-25 10:30:00",
      "canCancel": true
    },
    {
      "id": 1,
      "coachId": 1,
      "coachName": "张教练",
      "resignationDate": "2025-01-20",
      "reason": "身体原因",
      "status": "rejected",
      "statusDesc": "已拒绝",
      "applyTime": "2025-01-10 14:20:00",
      "reviewTime": "2025-01-12 09:15:00",
      "reviewRemark": "当前项目需要您，请再考虑",
      "canCancel": false
    }
  ]
}
```

#### 5.4 获取离职申请详情
- **接口**: `GET /app/coach/resignation/{applicationId}`
- **分组**: 05-教练离职管理
- **认证**: Bearer Token
- **参数**: 
  - `applicationId` (路径参数): 申请ID
- **请求示例**:
```
GET /app/coach/resignation/1
```
- **返回示例**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "coachId": 1,
    "coachName": "张教练",
    "resignationDate": "2025-02-15",
    "reason": "个人原因，需要照顾家庭",
    "handoverPlan": "将所有在约课程交接给李教练",
    "status": "approved",
    "statusDesc": "已批准",
    "applyTime": "2025-01-25 10:30:00",
    "reviewTime": "2025-01-26 15:20:00",
    "reviewRemark": "同意离职申请，感谢您的贡献",
    "actualLeaveDate": "2025-02-01",
    "canCancel": false
  }
}
```
- **错误场景**:
  - 申请不存在: "离职申请不存在"

#### 5.5 撤销离职申请
- **接口**: `DELETE /app/coach/resignation/{applicationId}/cancel`
- **分组**: 05-教练离职管理
- **认证**: Bearer Token
- **参数**: 
  - `applicationId` (路径参数): 申请ID
- **说明**: 只有待审核(pending)状态的申请才能撤销
- **请求示例**:
```
DELETE /app/coach/resignation/1/cancel
```
- **返回示例**:
```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```
- **错误场景**:
  - 申请不存在: "离职申请不存在"
  - 不能撤销: "该离职申请不能撤销"（已审核或已取消的申请）

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
- **请求示例**:
```
GET /admin/user-account-cancel/list?pageNum=1&pageSize=10&status=pending
```

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
- **审核结果**: 
  - `approved` - 已批准
  - `rejected` - 已拒绝

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
- **请求示例**:
```
GET /admin/coach/certification/list?page=1&size=10&status=pending&sortOrder=desc
```

##### 获取教练认证申请详情
- **接口**: `GET /admin/coach/certification/detail/{id}`
- **参数**: 申请ID
- **返回**: 包含用户信息、教练信息、证书列表、教育经历、工作经历等完整信息

##### 审核教练认证申请（JSON方式）
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
- **参数说明**:
  - `id`: 申请ID（必填）
  - `status`: 审核结果（必填）- approved已通过 / rejected已拒绝
  - `reviewRemark`: 审核备注（最多500字）
  - `certificationNo`: 认证编号（通过时可自定义，不填则自动生成）

##### 审核教练认证申请（表单方式）
- **接口**: `PUT /admin/coach/certification/review-form`
- **参数**:
  - `id`: 申请ID（必填）
  - `status`: 审核结果（必填）- approved已通过 / rejected已拒绝
  - `reviewRemark`: 审核备注
  - `certificationNo`: 认证编号
- **请求示例**:
```
PUT /admin/coach/certification/review-form?id=1&status=approved&reviewRemark=审核通过
```

##### 快捷批准教练认证申请
- **接口**: `PUT /admin/coach/certification/{id}/approve`
- **参数**: 
  - `id`: 申请ID（路径参数）
  - `reviewRemark`: 审核备注（可选，默认"申请材料齐全，符合教练认证要求"）
- **请求示例**:
```
PUT /admin/coach/certification/1/approve?reviewRemark=优秀教练，批准认证
```

##### 快捷拒绝教练认证申请
- **接口**: `PUT /admin/coach/certification/{id}/reject`
- **参数**: 
  - `id`: 申请ID（路径参数）
  - `reviewRemark`: 拒绝原因（必填）
- **请求示例**:
```
PUT /admin/coach/certification/1/reject?reviewRemark=证书不齐全，请补充相关资质证明
```

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
- **请求示例**:
```
GET /admin/coach/resignation/list?current=1&size=10&status=pending
```

##### 查看教练离职申请详情
- **接口**: `GET /admin/coach/resignation/{id}`
- **参数**: 申请ID
- **返回**: 离职申请详情，包含教练信息、申请原因、预计离职日期等

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
- **参数说明**:
  - `reviewRemark`: 审核备注（必填）
  - `actualLeaveDate`: 实际离职日期（可选，格式: YYYY-MM-DD）

##### 拒绝教练离职申请
- **接口**: `POST /admin/coach/resignation/reject/{id}`
- **参数**: 申请ID（路径参数）
- **请求示例**:
```json
{
  "reviewRemark": "当前项目需要您，请再考虑一下"
}
```
- **参数说明**:
  - `reviewRemark`: 审核备注/拒绝原因（必填）

##### 取消教练离职申请
- **接口**: `DELETE /admin/coach/resignation/cancel/{id}`
- **参数**: 
  - `id`: 申请ID（路径参数）
  - `cancelReason`: 取消原因（可选）
- **请求示例**:
```
DELETE /admin/coach/resignation/cancel/1?cancelReason=管理员取消该申请
```

---

## 📝 测试流程建议

### App端完整测试流程:

#### 教练离职完整测试用例:

**前置条件**: 用户已登录且已通过教练认证

**测试步骤**:

1. **提交离职申请**
```bash
POST http://localhost:8081/app/coach/resignation/apply
Headers: Authorization: Bearer {your_token}
Body:
{
  "resignationDate": "2025-03-01",
  "reason": "个人原因，需要照顾家庭",
  "handoverPlan": "将所有在约课程交接给李教练，预计3天完成交接"
}
```

2. **查看当前申请状态**
```bash
GET http://localhost:8081/app/coach/resignation/current-status
Headers: Authorization: Bearer {your_token}
```

3. **查看申请历史**
```bash
GET http://localhost:8081/app/coach/resignation/history
Headers: Authorization: Bearer {your_token}
```

4. **查看申请详情** (假设申请ID=1)
```bash
GET http://localhost:8081/app/coach/resignation/1
Headers: Authorization: Bearer {your_token}
```

5. **撤销申请** (可选，仅待审核状态可撤销)
```bash
DELETE http://localhost:8081/app/coach/resignation/1/cancel
Headers: Authorization: Bearer {your_token}
```

6. **Admin端审核** (在Admin系统中操作)
```bash
# 批准
POST http://localhost:8080/admin/coach/resignation/approve/1
Body: {"reviewRemark": "同意离职", "actualLeaveDate": "2025-02-28"}

# 或拒绝
POST http://localhost:8080/admin/coach/resignation/reject/1
Body: {"reviewRemark": "当前项目需要您"}
```

---

### App端完整测试流程:

1. **用户注册流程**:
   - 发送注册验证码 → 用户注册 → 登录获取token

2. **个人信息管理流程**:
   - 设置JWT认证 → 获取个人信息 → 修改个人信息 → 修改密码

3. **账号注销流程**:
   - 发送注销验证码 → 提交注销申请 → (可选)撤销申请

4. **教练认证流程**:
   - 检查申请资格 → 提交认证申请 → 查看申请状态

5. **教练离职流程** (完整测试):
   - 步骤1: 提交离职申请 `POST /app/coach/resignation/apply`
   - 步骤2: 查看当前申请状态 `GET /app/coach/resignation/current-status`
   - 步骤3: 查看申请历史 `GET /app/coach/resignation/history`
   - 步骤4: 查看申请详情 `GET /app/coach/resignation/{id}`
   - 步骤5: (可选)撤销申请 `DELETE /app/coach/resignation/{id}/cancel`
   - 步骤6: Admin端审核 `POST /admin/coach/resignation/approve/{id}` 或 `reject/{id}`

### Admin端完整测试流程:

1. **账号注销审核流程**:
   - 获取待审核列表 → 查看申请详情 → 审核申请(通过/拒绝)

2. **教练认证审核流程**:
   - 查询待审核认证申请列表 → 查看申请详情 → 审核认证申请
   - 审核通过: 系统自动创建教练账号、分配教练角色、生成认证编号
   - 审核拒绝: 用户可重新修改并提交申请

3. **教练离职审核流程**:
   - 查询离职申请列表 → 查看离职理由及详情 → 审核离职申请
   - 批准离职: 更新教练状态、移除教练角色、记录实际离职日期
   - 拒绝离职: 保持教练状态不变，教练可继续提供服务

---

## ⚠️ 注意事项

1. **JWT认证**:
   - App端所有需要认证的接口都需要先登录获取token
   - 在Knife4j界面右上角点击🔒设置认证
   - Token有效期为24小时，过期需要重新登录

2. **验证码**:
   - 开发模式下所有验证码统一为 `123456`
   - 验证码有效期为5分钟
   - 同一手机号60秒内只能发送一次

3. **账号注销**:
   - 临时注销：7天后生效，3天内可撤销
   - 永久注销：30天后生效，15天内可撤销
   - 注销申请需要管理员审核

4. **请求路径对应**:
   - App端认证接口: `/app/auth/**` (对应模块01-用户认证)
   - App端个人信息: `/app/profile/**` (对应模块02-个人信息管理)
   - App端角色管理: `/app/role/**` (对应模块03-角色管理)
   - App端教练认证: `/app/coach/certification/**` (对应模块04-教练认证申请)
   - App端教练离职: `/app/coach/resignation/**` (对应模块05-教练离职管理)
   - Admin端注销管理: `/admin/user-account-cancel/**` (对应模块01-用户权限管理)
   - Admin端教练认证: `/admin/coach/certification/**` (对应模块02-教练管理)
   - Admin端教练离职: `/admin/coach/resignation/**` (对应模块02-教练管理)

5. **教练认证和离职流程**:
   - 教练认证通过后：系统自动在coach表中创建教练记录，分配教练角色
   - 用户可多次申请：如果申请被拒绝或取消，用户可以重新提交申请
   - 离职后可重新认证：教练离职后可以再次申请认证成为教练
   - 数据库约束：`coach_certification_apply`和`coach`表的`user_id`字段不设置唯一约束

6. **教练离职申请**:
   - 只有教练身份的用户才能提交离职申请
   - 离职日期必须是未来日期
   - 只有待审核状态的申请可以撤销
   - 同一时间只能有一个待审核的离职申请
   - Admin端批准离职后，教练状态将更新为离职，教练角色将被移除

---

## 🔧 数据库表创建

请在MySQL中执行以下SQL创建用户账号注销申请表：

```sql
-- 用户账号注销申请表
DROP TABLE IF EXISTS `user_account_cancel_apply`;
CREATE TABLE `user_account_cancel_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `cancel_type` varchar(20) NOT NULL COMMENT '注销类型: temporary临时 permanent永久',
  `reason` varchar(500) NOT NULL COMMENT '注销原因',
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态: pending待审核 approved已批准 rejected已拒绝 cancelled已取消',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人 ID',
  `review_remark` varchar(500) DEFAULT NULL COMMENT '审核备注',
  `effective_time` datetime DEFAULT NULL COMMENT '生效时间',
  `actual_cancel_time` datetime DEFAULT NULL COMMENT '实际注销时间',
  `cancel_deadline` datetime DEFAULT NULL COMMENT '撤销截止时间',
  `is_cancellable` tinyint DEFAULT 1 COMMENT '是否可撤销: 0否 1是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_apply_time` (`apply_time`),
  KEY `idx_effective_time` (`effective_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账号注销申请表';
```

---

## 📚 更新日志

- 2025-01-25:
  - ✅ 修复Admin端教练离职申请管理SQL错误，更正表关联问题
  - ✅ 新增App端教练离职申请完整功能：提交申请、查看状态、撤销申请
  - ✅ 创建App端教练离职申请相关DTO、VO、Mapper、Service、Controller
  - ✅ 修复App模块编译错误：Jackson注解、UserContext、类型转换
  - ✅ 更新App端Knife4j配置，添加教练离职管理分组(05-教练离职管理)
  - ✅ 完善Admin端教练认证审核接口文档
  - ✅ 新增Admin端教练离职申请管理接口文档
  - ✅ 修复数据库唯一约束问题，支持用户多次申请教练认证
  - ✅ 更新测试流程，添加教练认证和离职审核流程
  - ✅ 完整更新App端教练离职管理接口文档，包含5个接口详细说明
  - ✅ 添加完整测试用例和请求/响应示例

- 2025-01-24: 
  - ✅ 修复短信登录功能，支持密码登录和短信登录两种方式
  - ✅ 添加 `cancel_account` 验证码类型支持
  - ✅ 完善用户账号注销申请功能，申请记录保存到数据库
  - ✅ 新增Admin端账号注销审核管理接口
  - ✅ 更新Knife4j配置，正确映射所有接口到对应模块
