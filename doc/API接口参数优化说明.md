# API接口参数优化说明

## 概述
为了提升在线API文档（Swagger/Knife4j）的用户体验，特别是参数传递的便利性，我们为原有的JSON请求体接口增加了对应的表单参数版本，使开发者可以更方便地在API文档界面中进行接口测试。

## 优化内容

### 1. 用户管理相关接口（UserController）

#### 1.1 用户角色分配接口
- **新增接口**: `POST /admin/user/assign-roles-form`
- **参数类型**: 表单参数
- **参数说明**:
  - `userId` (Long): 用户ID，必填
  - `roleIds` (String): 角色ID列表，逗号分隔，必填
  - `remark` (String): 备注，可选
- **原接口**: `POST /admin/user/assign-roles` (JSON请求体)
- **优势**: 在API文档中可以直接选择用户ID和角色ID，无需手动编写JSON

#### 1.2 用户状态更新接口
- **新增接口**: `PUT /admin/user/status-form`
- **参数类型**: 表单参数
- **参数说明**:
  - `userId` (Long): 用户ID，必填
  - `status` (Integer): 用户状态，0-禁用/1-正常，必填
  - `remark` (String): 备注，可选
- **原接口**: `PUT /admin/user/status` (JSON请求体)
- **优势**: 状态参数可以使用下拉选择，操作更直观

### 2. 用户角色关系管理接口（UserRoleController）

#### 2.1 批量用户角色分配接口
- **新增接口**: `POST /admin/user-role/batch-assign-form`
- **参数类型**: 表单参数
- **参数说明**:
  - `userIds` (String): 用户ID列表，逗号分隔，必填
  - `roleIds` (String): 角色ID列表，逗号分隔，必填
  - `operation` (String): 操作类型，replace/add/remove，必填
  - `remark` (String): 备注，可选
- **原接口**: `POST /admin/user-role/batch-assign` (JSON请求体)
- **优势**: 操作类型可以使用下拉选择，支持批量操作

### 3. 角色管理相关接口（RoleController）

#### 3.1 角色状态更新接口
- **新增接口**: `PUT /admin/role/status-form`
- **参数类型**: 表单参数
- **参数说明**:
  - `roleId` (Long): 角色ID，必填
  - `status` (Integer): 角色状态，0-禁用/1-正常，必填
  - `remark` (String): 备注，可选
- **原接口**: `PUT /admin/role/status` (JSON请求体)
- **优势**: 状态参数可以使用下拉选择，便于快速测试

## 技术实现

### 参数验证
所有新增的表单参数接口都保持了与原接口相同的参数验证逻辑：
- 使用`@NotNull`验证必填参数
- 使用`@Min`验证数值范围
- 对枚举值进行业务逻辑验证

### 参数解析
- **ID列表解析**: 逗号分隔的字符串转换为Long列表
- **状态值验证**: 确保状态值只能为0或1
- **操作类型验证**: 确保操作类型在允许范围内

### 错误处理
保持与原接口一致的错误处理机制：
- 参数验证失败返回具体错误信息
- 业务逻辑错误返回相应提示
- 系统异常统一处理并记录日志

## 向后兼容性
- 原有JSON请求体接口保持不变，确保现有客户端正常工作
- 新增的表单参数接口作为补充，不影响现有功能
- 两种接口类型可以并存使用

## 使用建议
1. **开发测试阶段**: 推荐使用表单参数版本接口，便于在API文档中快速测试
2. **前端集成**: 可以根据具体需求选择合适的接口类型
3. **自动化测试**: JSON接口更适合自动化测试脚本

## API文档访问
- **Admin端API文档**: `/admin/doc.html`
- **App端API文档**: `/app/doc.html`

通过以上优化，开发者可以在API文档界面中更加便捷地进行接口测试，提高开发效率和用户体验。