# 健身平台教练日程管理API完整测试指南

## 📋 概述

本指南提供健身平台教练日程管理功能的完整API测试方法，包括App端的日程管理和Admin端的日程审核功能。所有接口路径已与Swagger配置完全匹配。

## 🌐 服务端口和访问地址

- **App端（用户端）：** http://localhost:8081
- **Admin端（管理端）：** http://localhost:8080
- **App端API文档：** http://localhost:8081/doc.html
- **Admin端API文档：** http://localhost:8080/doc.html

## 🔄 启动应用

```bash
# 启动App端
mvn spring-boot:run -f D:\DevelopmentLOOK\Idea\idea_project_workspace\huang-parent\web\web-app\pom.xml

# 启动Admin端（新开命令窗口）
mvn spring-boot:run -f D:\DevelopmentLOOK\Idea\idea_project_workspace\huang-parent\web\web-admin\pom.xml
```

## 🔍 Swagger配置验证

### App端配置验证
- **接口路径：** `/app/coach/schedule/**`
- **Swagger分组：** `07-教练日程管理`
- **配置文件：** `web-app/src/main/java/com/huang/web/app/custom/config/Knife4jConfiguration.java`
- **匹配状态：** ✅ 已验证匹配

### Admin端配置验证
- **接口路径：** `/admin/coach/schedule/**`
- **Swagger分组：** `02-教练管理`
- **配置文件：** `web-admin/src/main/java/com/huang/web/admin/custom/config/Knife4jConfiguration.java`
- **匹配状态：** ✅ 已验证匹配

## 🛠️ API接口功能

### 📱 App端教练日程管理功能

在Knife4j文档 http://localhost:8081/doc.html 中找到"**07-教练日程管理**"分组，包含以下接口：

> ℹ️ **重要：** 所有App端接口都需要JWT Bearer Token认证

#### 1. 教练可用性管理

##### 1.1 查询教练可用性列表
- **接口路径：** `GET /app/coach/schedule/availability/list`
- **功能：** 查询当前教练的一周可用时间设置
- **认证：** 需要Bearer Token
- **请求示例：**
  ```
  GET /app/coach/schedule/availability/list
  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
  ```

##### 1.2 设置教练可用性
- **接口路径：** `POST /app/coach/schedule/availability/set`
- **功能：** 设置教练在某个星期几的可用时间
- **认证：** 需要Bearer Token
- **请求参数：**
  ```json
  {
    "dayOfWeek": 1,
    "startTime": "09:00:00",
    "endTime": "18:00:00",
    "isAvailable": 1
  }
  ```
- **参数说明：**
  - `dayOfWeek`: 星期几(1-7，1为周一，7为周日)
  - `startTime`: 开始时间(格式：HH:mm:ss)
  - `endTime`: 结束时间(格式：HH:mm:ss)
  - `isAvailable`: 是否可用(1可用，0不可用)

##### 1.3 删除教练可用性设置
- **接口路径：** `DELETE /app/coach/schedule/availability/{id}`
- **功能：** 删除教练的可用性设置
- **认证：** 需要Bearer Token

#### 2. 教练日程变更申请

##### 2.1 申请日程变更
- **接口路径：** `POST /app/coach/schedule/change/apply`
- **功能：** 教练申请日程变更（请假、加班、调整）
- **认证：** 需要Bearer Token
- **请求参数：**
  ```json
  {
    "changeType": "leave",
    "originalDate": "2025-10-15",
    "originalStartTime": "09:00:00",
    "originalEndTime": "12:00:00",
    "reason": "有急事需要请假"
  }
  ```
- **变更类型说明：**
  - `leave`: 请假
  - `overtime`: 加班
  - `adjust`: 调整

##### 2.2 分页查询日程变更申请
- **接口路径：** `GET /app/coach/schedule/change/page`
- **功能：** 分页查询当前教练的日程变更申请记录
- **认证：** 需要Bearer Token
- **查询参数：**
  - `current`: 当前页码(默认1)
  - `size`: 每页大小(默认10)
  - `status`: 状态筛选(pending/approved/rejected)
  - `changeType`: 变更类型(leave/overtime/adjust)
  - `startDate`: 开始日期
  - `endDate`: 结束日期

##### 2.3 查询日程变更申请详情
- **接口路径：** `GET /app/coach/schedule/change/{id}`
- **功能：** 根据ID查询日程变更申请详情
- **认证：** 需要Bearer Token

##### 2.4 取消日程变更申请
- **接口路径：** `DELETE /app/coach/schedule/change/{id}`
- **功能：** 取消待审核状态的日程变更申请
- **认证：** 需要Bearer Token

### 💼 Admin端教练日程审核功能

在Knife4j文档 http://localhost:8080/doc.html 中找到"**02-教练管理**"分组，包含以下日程审核接口：

> ℹ️ **重要：** 审核接口需要管理员JWT Bearer Token认证

#### 1. 分页查询日程变更申请
- **接口路径：** `GET /admin/coach/schedule/change/page`
- **功能：** 分页查询所有教练的日程变更申请记录
- **查询参数：**
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

#### 2. 查询日程变更申请详情
- **接口路径：** `GET /admin/coach/schedule/change/{id}`
- **功能：** 根据ID查询日程变更申请详情，包含教练信息

#### 3. 审核日程变更申请
- **接口路径：** `POST /admin/coach/schedule/change/review`
- **功能：** 审核教练的日程变更申请
- **认证：** 需要Bearer Token（管理员）
- **请求参数：**
  ```json
  {
    "id": 1,
    "status": "approved",
    "reviewRemark": "同意请假申请"
  }
  ```
- **参数说明：**
  - `id`: 申请ID
  - `status`: 审核结果(approved已批准/rejected已拒绝)
  - `reviewRemark`: 审核备注

## 🗨️ 完整测试流程

### 📱 1. App端测试流程

#### 步骤1：教练账号登录

**可用教练账号：**
- `coach001` / `123456` (李教练)
- `coach002` / `123456` (王教练)
- `coach003` / `123456` (张教练)
- `coach004` / `123456` (刘教练)

**登录步骤：**
1. 访问 App端登录接口
2. 获取JWT Token
3. 在Swagger中点击"Authorize"按钮设置Bearer Token

#### 步骤2：设置教练可用性

**调用接口：** `POST /app/coach/schedule/availability/set`

**示例数据：**
```json
// 设置周一工作时间
{
  "dayOfWeek": 1,
  "startTime": "09:00:00",
  "endTime": "18:00:00",
  "isAvailable": 1
}

// 设置周二工作时间
{
  "dayOfWeek": 2,
  "startTime": "09:00:00",
  "endTime": "18:00:00",
  "isAvailable": 1
}
```

#### 步骤3：申请日程变更

**调用接口：** `POST /app/coach/schedule/change/apply`

**请假申请示例：**
```json
{
  "changeType": "leave",
  "originalDate": "2025-10-15",
  "originalStartTime": "09:00:00",
  "originalEndTime": "12:00:00",
  "reason": "有急事需要请假半天"
}
```

**加班申请示例：**
```json
{
  "changeType": "overtime",
  "originalDate": "2025-10-16",
  "newStartTime": "19:00:00",
  "newEndTime": "21:00:00",
  "reason": "应会员要求加班服务"
}
```

#### 步骤4：查看申请记录

**调用接口：** `GET /app/coach/schedule/change/page`

**查询参数示例：**
- `current=1&size=10` (查看第1页，每页10条)
- `status=pending` (只查看待审核的申请)
- `changeType=leave` (只查看请假申请)

### 💼 2. Admin端测试流程

#### 步骤1：管理员登录

**管理员账号：** `admin` / `123456`

**登录步骤：**
1. 访问 Admin端登录接口
2. 获取JWT Token
3. 在Swagger中设置Bearer Token

#### 步骤2：查看待审核申请

**调用接口：** `GET /admin/coach/schedule/change/page`

**查询参数示例：**
- `status=pending` (查看待审核申请)
- `coachName=李教练` (查看指定教练的申请)
- `changeType=leave` (查看请假申请)
- `current=1&size=10` (分页查询)

#### 步骤3：审核申请

**调用接口：** `POST /admin/coach/schedule/change/review`

**批准申请示例：**
```json
{
  "id": 1,
  "status": "approved",
  "reviewRemark": "同意请假申请，已安排代课教练"
}
```

**拒绝申请示例：**
```json
{
  "id": 2,
  "status": "rejected",
  "reviewRemark": "当日已有课程安排，无法批准请假"
}
```

### 🔄 3. 跨端联动测试

#### 完整测试场景

**场景1：请假申请流程**
1. 📱 **App端：** coach001登录 → 申请请假
2. 💼 **Admin端：** admin登录 → 查看申请 → 批准申请
3. 📱 **App端：** 查看申请状态变为“已批准”

**场景2：加班申请流程**
1. 📱 **App端：** coach002登录 → 申请加班
2. 💼 **Admin端：** admin登录 → 查看申请 → 拒绝申请
3. 📱 **App端：** 查看申请状态变为“已拒绝”

#### 权限控制测试
- **教练权限：** 只能查看和管理自己的申请
- **管理员权限：** 可以查看和审核所有教练的申请
- **状态限制：** 只有“待审核”状态的申请可以取消/审核

## 数据准备

### 测试账号
- **教练账号：** coach001/123456, coach002/123456
- **管理员账号：** admin/123456

### 测试数据
数据库已包含测试数据，无需额外准备。

## 注意事项

1. **认证要求：** 所有接口都需要Bearer Token认证
2. **权限控制：** 教练只能管理自己的日程，管理员可以审核所有申请
3. **状态管理：** 只有待审核状态的申请可以取消或审核
4. **时间格式：** 时间参数使用标准格式(HH:mm:ss, yyyy-MM-dd)
5. **分页查询：** 支持多种筛选条件，结果按申请时间倒序排列

## API响应示例

### 成功响应
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "coachName": "李教练",
    "changeType": "leave",
    "changeTypeName": "请假",
    "status": "pending",
    "statusName": "待审核"
  },
  "success": true,
  "timestamp": 1697097600000
}
```

### 错误响应
```json
{
  "code": 500,
  "message": "您不是认证教练，无法申请日程变更",
  "data": null,
  "success": false,
  "timestamp": 1697097600000
}
```

## ❓ 常见问题及解决方案

### 认证相关问题

**Q1: 教练无法申请日程变更？**
✅ **解决方案：**
- 确保使用已认证的教练账号（coach001-004）
- 检查JWT Token是否正确设置
- 确认教练状态为激活状态

**Q2: Bearer Token认证失败？**
✅ **解决方案：**
- 检查Token格式：`Bearer eyJhbGciOiJIUzI1NiJ9...`
- 确保Token未过期，重新登录获取
- 在Swagger中点击Authorize按钮设置

### 业务逻辑问题

**Q3: 无法取消/审核申请？**
✅ **解决方案：**
- 只有“待审核(pending)”状态可以取消/审核
- 已审核的申请无法修改
- 教练只能取消自己的申请

**Q4: 管理员查不到申请记录？**
✅ **解决方案：**
- 检查是否有教练提交的申请
- 确认管理员权限正常
- 检查查询参数是否正确

### 数据格式问题

**Q5: 时间格式错误？**
✅ **解决方案：**
- 日期格式：`yyyy-MM-dd` 例：2025-10-15
- 时间格式：`HH:mm:ss` 例：09:00:00
- 星期格式：1-7 (周一到周日)

**Q6: JSON参数验证失败？**
✅ **解决方案：**
- 检查必填字段是否缺失
- 确保数据类型正确
- 检查枚举值是否合法

## 🔧 故障排除步骤

### 1. 检查应用启动状态
```bash
# 检查端口占用
netstat -ano | findstr :8080
netstat -ano | findstr :8081

# 检查应用日志
tail -f web-app/logs/application.log
tail -f web-admin/logs/application.log
```

### 2. 检查数据库连接
```sql
-- 检查表是否存在
SHOW TABLES LIKE 'coach_schedule_change';
SHOW TABLES LIKE 'coach_availability';

-- 检查测试数据
SELECT * FROM coach_schedule_change LIMIT 5;
SELECT * FROM coach_availability LIMIT 5;
```

### 3. API调用测试
```bash
# 使用curl测试接口
curl -X GET "http://localhost:8081/doc.html"
curl -X GET "http://localhost:8080/doc.html"

# 测试健康检查接口
curl -X GET "http://localhost:8081/actuator/health"
curl -X GET "http://localhost:8080/actuator/health"
```

## 📈 性能监控

### 关键指标
- **响应时间：** < 200ms
- **并发请求：** 支持100+并发
- **数据库连接：** 连接池监控
- **内存使用：** JVM堆内存监控

### 性能测试建议
1. **单接口测试：** 使用Postman或JMeter
2. **并发测试：** 模拟多教练同时申请
3. **压力测试：** 长时间高负载测试

## 🚀 快速开始指南

### 第一次使用？请按步骤操作：

1. ✅ **启动应用**
   ```bash
   # 终端1: 启动App端
   mvn spring-boot:run -f web/web-app/pom.xml
   
   # 终端2: 启动Admin端
   mvn spring-boot:run -f web/web-admin/pom.xml
   ```

2. ✅ **检查服务状态**
   - App端: http://localhost:8081/doc.html
   - Admin端: http://localhost:8080/doc.html

3. ✅ **第一个测试**
   - 使用 `coach001/123456` 登录App端
   - 设置Bearer Token
   - 申请一个请假

4. ✅ **审核测试**
   - 使用 `admin/123456` 登录Admin端
   - 查看并审核申请

## 📊 功能覆盖范围

| 功能模块 | App端 | Admin端 | 状态 |
|---------|--------|----------|------|
| 教练可用性管理 | ✅ | - | 完成 |
| 日程变更申请 | ✅ | - | 完成 |
| 申请记录查询 | ✅ | ✅ | 完成 |
| 申请审核管理 | - | ✅ | 完成 |
| 权限控制 | ✅ | ✅ | 完成 |
| JWT认证 | ✅ | ✅ | 完成 |
| Swagger文档 | ✅ | ✅ | 完成 |

## 👏 成功指标

通过以下测试表明功能完善：

- ✅ 教练可以成功设置工作时间
- ✅ 教练可以成功提交日程变更申请
- ✅ 管理员可以查看所有申请
- ✅ 管理员可以成功审核申请
- ✅ 申请状态正确流转
- ✅ 权限控制正常生效
- ✅ 数据校验正常工作
- ✅ 错误处理机制完善

---

## 🎉 总结

**本文档提供了健身平台教练日程管理API的完整测试指南**，包括：

- ✨ **全面的接口说明** - 所有接口路径已与Swagger配置验证匹配
- 📝 **详细的测试步骤** - 从应用启动到完整测试流程
- 🛠️ **实用的故障排除** - 常见问题及解决方案
- 📈 **性能监控建议** - 帮助优化系统性能

通过详细的测试验证，可以确保教练日程管理功能的**正确性、完整性和可靠性**。
