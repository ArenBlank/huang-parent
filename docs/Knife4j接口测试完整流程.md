# Knife4j接口测试完整流程

## 访问地址

- **Admin端**: `http://localhost:8080/doc.html`
- **App端**: `http://localhost:8081/doc.html`

---

## Admin端接口测试流程 (端口8080)

### 📋 测试前准备
1. 确保Admin服务已启动 (端口8080)
2. 数据库已初始化 (使用`1_fitness_platform_complete.sql`)

### 🔐 1. 用户管理模块

#### 1.1 用户列表查询
**分组**: `用户管理`
**接口**: `GET /admin/user/list`

**测试参数**:
```json
{
  "page": 1,
  "size": 10,
  "username": "",
  "phone": "",
  "status": null,
  "startTime": "",
  "endTime": "",
  "sortField": "createTime",
  "sortOrder": "desc"
}
```

**预期结果**: 返回分页用户列表

#### 1.2 用户详情查询
**接口**: `GET /admin/user/detail/{userId}`

**测试参数**:
- Path参数: `userId = 1`

**预期结果**: 返回ID为1的用户详细信息

#### 1.3 用户状态管理
**接口**: `PUT /admin/user/status-form`

**测试参数**:
```
userId: 2
status: 0
remark: 测试禁用用户
```

**预期结果**: 用户状态更新成功

### 🎯 2. 角色管理模块

#### 2.1 角色列表查询
**分组**: `角色管理`
**接口**: `GET /admin/role/list`

**测试参数**:
```json
{
  "page": 1,
  "size": 10,
  "roleName": "",
  "roleCode": "",
  "status": null,
  "startTime": "",
  "endTime": "",
  "sortField": "createTime",
  "sortOrder": "desc"
}
```

**预期结果**: 返回分页角色列表

#### 2.2 获取可用角色列表
**接口**: `GET /admin/role/available`

**测试参数**: 无

**预期结果**: 返回所有启用状态的角色

#### 2.3 角色统计信息
**接口**: `GET /admin/role/statistics`

**测试参数**: 无

**预期结果**: 返回角色相关统计数据

### 👥 3. 用户角色管理

#### 3.1 用户角色分配 (JSON格式)
**分组**: `用户角色管理`
**接口**: `POST /admin/user-role/assign`

**测试参数**:
```json
{
  "userId": 1,
  "roleIds": [2, 3]
}
```

**预期结果**: 用户角色分配成功

#### 3.2 用户角色分配 (表单格式)
**接口**: `POST /admin/user-role/assign-form`

**测试参数**:
```
userId: 1
roleIds: 2,3
```

**预期结果**: 用户角色分配成功

#### 3.3 批量角色分配
**接口**: `POST /admin/user-role/batch-assign`

**测试参数**:
```json
{
  "userIds": [1, 2, 3],
  "roleIds": [2]
}
```

**预期结果**: 批量角色分配成功

---

## App端接口测试流程 (端口8081)

### 📋 测试前准备
1. 确保App服务已启动 (端口8081)
2. 数据库已初始化

### 🔐 1. 用户认证模块

#### 1.1 发送短信验证码
**分组**: `01-用户认证`
**接口**: `POST /app/auth/sms-code/send`

**测试参数**:
```json
{
  "phone": "13888888888",
  "type": "register",
  "captcha": "abcd",
  "captchaKey": "captcha_key_123"
}
```

**预期结果**: 验证码发送成功，控制台显示验证码123456

#### 1.2 用户注册
**接口**: `POST /app/auth/register`

**测试参数** (注册前需先发送验证码):
```json
{
  "username": "testuser001",
  "phone": "13888888888",
  "password": "123456",
  "confirmPassword": "123456",
  "smsCode": "123456",
  "nickname": "测试用户001",
  "email": "testuser001@example.com",
  "gender": 1,
  "birthDate": "1990-01-01",
  "inviteCode": ""
}
```

**预期结果**: 用户注册成功

#### 1.3 密码登录
**接口**: `POST /app/auth/login`

**测试参数**:
```json
{
  "account": "testuser001",
  "password": "123456",
  "loginType": "password",
  "deviceId": "device_test_001",
  "deviceType": "web"
}
```

**预期结果**: 登录成功，返回JWT token

#### 1.4 短信验证码登录
**步骤1**: 先发送登录验证码
```json
{
  "phone": "13888888888",
  "type": "login",
  "captcha": "abcd",
  "captchaKey": "captcha_key_123"
}
```

**步骤2**: 短信登录（使用虚拟密码绕过验证）
```json
{
  "account": "13888888888",
  "password": "dummy_password",
  "loginType": "sms",
  "smsCode": "123456"
}
```

**注意**: 由于DTO验证问题，短信登录时需要提供一个虚拟密码绕过验证，但业务逻辑中只会验证短信验证码。

**预期结果**: 登录成功，返回JWT token

#### 1.5 刷新令牌
**接口**: `POST /app/auth/refresh-token`

**测试参数** (使用登录返回的refresh token):
```json
{
  "refreshToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
}
```

**预期结果**: 返回新的access token

#### 1.6 忘记密码
**步骤1**: 发送重置密码验证码
```json
{
  "phone": "13888888888",
  "type": "reset_password",
  "captcha": "abcd",
  "captchaKey": "captcha_key_123"
}
```

**步骤2**: 重置密码
**接口**: `POST /app/auth/forget-password`
```json
{
  "phone": "13888888888",
  "smsCode": "123456",
  "newPassword": "newpass123",
  "confirmPassword": "newpass123"
}
```

**预期结果**: 密码重置成功

#### 1.7 退出登录
**接口**: `POST /app/auth/logout`

**Headers**: 
```
Authorization: Bearer {刚才登录获得的token}
```

**测试参数**: 无

**预期结果**: 退出登录成功

### 👤 2. 个人信息管理模块

#### 2.1 获取个人信息
**分组**: `02-个人信息管理`
**接口**: `GET /app/profile/info`

**Headers** (所有个人信息接口都需要):
```
Authorization: Bearer {登录token}
```

**测试参数**: 无

**预期结果**: 返回当前登录用户的详细信息

#### 2.2 更新个人信息
**接口**: `PUT /app/profile/update`

**测试参数**:
```json
{
  "nickname": "更新后的昵称",
  "avatar": "http://example.com/avatar.jpg",
  "gender": 1,
  "birthday": "1990-01-01",
  "height": 175.5,
  "weight": 70.0,
  "introduction": "这是个人简介"
}
```

**预期结果**: 个人信息更新成功

#### 2.3 修改密码
**接口**: `PUT /app/profile/change-password`

**测试参数**:
```json
{
  "oldPassword": "123456",
  "newPassword": "newpass123",
  "confirmPassword": "newpass123"
}
```

**预期结果**: 密码修改成功

#### 2.4 绑定手机号
**步骤1**: 发送验证码 (使用认证模块的接口)
**步骤2**: 绑定手机号

**接口**: `PUT /app/profile/bind-phone`
```json
{
  "phone": "13900139001",
  "smsCode": "123456"
}
```

**预期结果**: 手机号绑定成功

### 🎯 3. 角色管理模块 (App端)

#### 3.1 角色列表查询
**分组**: `12-角色管理`
**接口**: `GET /app/role/list`

**测试参数**:
```json
{
  "page": 1,
  "size": 10,
  "roleName": "",
  "roleCode": "",
  "status": null,
  "startTime": "",
  "endTime": "",
  "sortOrder": "desc"
}
```

**预期结果**: 返回分页角色列表

#### 3.2 获取可用角色列表
**接口**: `GET /app/role/available`

**测试参数**: 无

**预期结果**: 返回所有启用状态的角色

---

## 📋 完整测试顺序建议

### Admin端测试顺序:
1. 角色管理 → 角色列表查询
2. 角色管理 → 获取可用角色
3. 用户管理 → 用户列表查询
4. 用户角色管理 → 用户角色分配
5. 用户管理 → 用户状态管理

### App端测试顺序:
1. **认证流程**:
   - 发送注册验证码 → 用户注册 → 密码登录
2. **个人信息管理**:
   - 获取个人信息 → 更新个人信息 → 修改密码
3. **其他功能**:
   - 角色列表查询 → 退出登录

## ⚠️ 注意事项

### 1. 验证码频率限制
- 同一手机号10秒内只能发送一次验证码
- 如果提示频率限制，等待10秒后重试

### 2. 参数格式要求
- 所有枚举值都是**小写** (password, sms, register, login等)
- JSON格式要正确，注意双引号

### 3. 权限验证
- 个人信息相关接口需要Bearer token
- token从登录接口获取

### 4. 数据库状态
- 测试前确保数据库有初始数据
- 如果用户已存在，注册会失败

### 5. 开发模式
- 短信验证码固定为123456
- 不会真实发送短信

## 🔍 错误排查

### 常见错误:
1. **400 参数验证失败**: 检查参数格式和必填字段
2. **验证码错误**: 确保使用123456，注意验证码类型匹配
3. **用户已存在**: 更换username或phone
4. **token无效**: 重新登录获取新token

### 测试技巧:
1. 先用Knife4j提供的默认示例测试
2. 成功后再修改为自定义参数
3. 按照建议的测试顺序进行
4. 保存成功的token用于后续接口测试