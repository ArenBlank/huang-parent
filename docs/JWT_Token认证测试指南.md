# JWT Token认证测试指南

## 🔐 正确的Token使用方法

### 在Knife4j中设置Authorization

#### ✅ 方法1: 使用Authorize按钮（推荐）

1. **登录获取Token**
   - 先调用登录接口获取 `accessToken`

2. **设置Authorization**
   - 在Knife4j页面右上角点击 🔒 **"Authorize"** 按钮
   - 在输入框中输入：
     ```
     Bearer eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoiYWNjZXNzX3Rva2VuIiwidXNlcklkIjoxMywicGxhdGZvcm0iOiJhcHAiLCJ1c2VybmFtZSI6InRlc3R1c2VyMDAxIiwic3ViIjoidGVzdHVzZXIwMDEiLCJpYXQiOjE3NTk4NTE1MTMsImV4cCI6MTc2MDQ1NjMxM30.6pNNwPZDUtYjVgrp3JDVaZr0WT5GF1FA7ooHuOWJmmw
     ```
   - 点击"Authorize"确认

3. **测试需要认证的接口**
   - 之后所有需要认证的接口都会自动携带这个token

#### ✅ 方法2: 手动添加Header

如果要手动添加请求头：

**错误示例** ❌:
```
Header键: access-token
Header值: eyJhbGciOiJIUzI1NiJ9...
```

**正确示例** ✅:
```
Header键: Authorization  
Header值: Bearer eyJhbGciOiJIUzI1NiJ9...
```

## 🧪 完整测试流程

### 步骤1: 登录获取Token

```bash
POST /app/auth/login
{
  "account": "testuser001",
  "password": "123456", 
  "loginType": "password"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "userInfo": {...},
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...", 
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

### 步骤2: 设置Authorization

复制上面的 `accessToken`，在Knife4j中设置：
```
Bearer eyJhbGciOiJIUzI1NiJ9...
```

### 步骤3: 测试需要认证的接口

```bash
GET /app/profile/info
```

**预期响应**: 用户信息

### 步骤4: 测试退出登录

```bash
POST /app/auth/logout
```

**预期响应**: "退出登录成功"

### 步骤5: 用已退出的Token测试

再次调用 `/app/profile/info`

**预期响应**:
```json
{
  "code": 501,
  "message": "令牌已失效，请重新登录",
  "data": null
}
```

## 🚨 常见错误及解决方案

### 错误1: 未提供有效的访问令牌 (501)

**原因**: Authorization头格式错误

**解决方案**: 
- 确保Header键是 `Authorization`
- 确保Header值以 `Bearer ` 开头（注意空格）

### 错误2: 访问令牌无效或已过期 (501)

**原因**: Token格式错误或已过期

**解决方案**: 
- 检查token完整性
- 重新登录获取新token

### 错误3: 令牌已失效，请重新登录 (501)

**原因**: Token已在黑名单中（已退出登录）

**解决方案**: 
- 重新登录获取新token

## 📋 需要认证的接口列表

根据过滤器白名单，以下接口**需要**认证：

- ✅ `GET /app/profile/info` - 获取个人信息
- ✅ `PUT /app/profile/update` - 更新个人信息  
- ✅ `PUT /app/profile/change-password` - 修改密码
- ✅ `PUT /app/profile/bind-phone` - 绑定手机号
- ✅ `POST /app/auth/logout` - 退出登录

以下接口**不需要**认证（白名单）：

- ❌ `POST /app/auth/login` - 登录
- ❌ `POST /app/auth/register` - 注册
- ❌ `POST /app/auth/sms-code/send` - 发送验证码
- ❌ `POST /app/auth/forget-password` - 忘记密码
- ❌ `GET /app/role/list` - 角色列表
- ❌ `GET /app/role/available` - 可用角色

## 🔍 调试技巧

### 检查Token格式

你的Token应该类似这样：
```
eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoiYWNjZXNzX3Rva2VuIiwidXNlcklkIjoxMywidXNlcm5hbWUiOiJ0ZXN0dXNlcjAwMSJ9.signature
```

### 在浏览器开发者工具中检查

1. 打开开发者工具 (F12)
2. 切换到Network标签
3. 发送请求
4. 检查请求头中的Authorization字段

### 验证Token有效期

你的token过期时间是: `1760456313` (时间戳)
转换为日期: 约2025年12月24日

如果当前时间超过这个时间，token就会过期。

## 💡 最佳实践

1. **使用accessToken进行API调用**
2. **Token过期时使用refreshToken获取新的accessToken**
3. **退出登录后立即清除本地存储的Token**
4. **在生产环境中确保HTTPS传输**

现在按照这个指南重新测试，应该就可以正常工作了！