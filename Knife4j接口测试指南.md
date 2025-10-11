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

## 📝 测试流程建议

### App端完整测试流程:

1. **用户注册流程**:
   - 发送注册验证码 → 用户注册 → 登录获取token

2. **个人信息管理流程**:
   - 设置JWT认证 → 获取个人信息 → 修改个人信息 → 修改密码

3. **账号注销流程**:
   - 发送注销验证码 → 提交注销申请 → (可选)撤销申请

4. **教练认证流程**:
   - 检查申请资格 → 提交认证申请 → 查看申请状态

### Admin端完整测试流程:

1. **账号注销审核流程**:
   - 获取待审核列表 → 查看申请详情 → 审核申请(通过/拒绝)

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
   - Admin端注销管理: `/admin/user-account-cancel/**` (对应模块01-用户权限管理)

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

- 2025-01-24: 
  - ✅ 修复短信登录功能，支持密码登录和短信登录两种方式
  - ✅ 添加 `cancel_account` 验证码类型支持
  - ✅ 完善用户账号注销申请功能，申请记录保存到数据库
  - ✅ 新增Admin端账号注销审核管理接口
  - ✅ 更新Knife4j配置，正确映射所有接口到对应模块
