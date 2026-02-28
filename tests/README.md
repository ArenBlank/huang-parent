# API回归测试（Postman + Newman）

## 1. 准备
- 确保中间件与服务已启动：
  - admin: `http://localhost:8080`
  - app: `http://localhost:8081`
- 修改 `tests/local.postman_environment.json` 中的 `videoFilePath` 为你本机真实视频路径。

可选：每次回归前重置测试数据（推荐）
```powershell
powershell -ExecutionPolicy Bypass -File tests/reset-test-data.ps1
```

## 2. 导入Postman
1. 导入集合：`tests/fitness-platform.postman_collection.json`
2. 导入环境：`tests/local.postman_environment.json`
3. 选择环境 `fitness-local`
4. 点击 `Run collection`

## 3. 一键命令行运行（推荐）
```powershell
npm install -g newman
newman run tests/fitness-platform.postman_collection.json -e tests/local.postman_environment.json
```

## 4. 覆盖用例
1. 登录
2. 订阅计划
3. 打卡
4. 查询教练档期
5. 创建预约
6. 模拟异步回调（首次）
7. 模拟异步回调（重复，幂等）
8. 超时关单任务触发
9. 视频上传
10. 新增视频素材
11. 计划项绑定视频
12. Admin 课程创建
13. Admin 课程上架
14. Admin 创建课程排期
15. 课程列表
16. 课程排期
17. 课程报名下单
18. 课程模拟异步回调
19. 课程退款
20. 我的课程报名记录（校验退款状态）
21. Admin 退款审计列表
22. Admin 支付回调审计列表
23. 计划详情校验视频播放地址

## 5. 档期占满说明
- 集合已内置“无可用档期时自动跳过预约链路（05/06/07）”逻辑，避免出现脚本报错中断。
- 若你希望每次都完整覆盖“预约+支付幂等”，请先重置测试数据，例如：
```sql
UPDATE coach_schedule SET booked_count = 0, status = 1 WHERE id IN (1,2);
```

## 6. 常见问题
- 上传报 `Maximum upload size exceeded`：
  - `application.yml` 增加
    - `spring.servlet.multipart.max-file-size: 200MB`
    - `spring.servlet.multipart.max-request-size: 200MB`
- 上传报文件不存在：
  - 检查 `videoFilePath` 是否是本机绝对路径。
