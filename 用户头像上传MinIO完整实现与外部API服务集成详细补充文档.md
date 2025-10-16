# 用户头像上传MinIO完整实现与外部API服务集成详细补充文档

> **文档版本**: v1.1
> **创建日期**: 2025-01-24
> **补充内容**: 用户头像上传MinIO完整逻辑 + 外部API服务超详细集成方案

---

## 📋 本文档内容

- [一、用户头像上传MinIO完整实现方案](#一用户头像上传minio完整实现方案)
- [二、外部API服务集成超详细指南](#二外部api服务集成超详细指南)

---

## 一、用户头像上传MinIO完整实现方案

### 1.1 功能需求分析

**业务场景**：
- 用户在App端个人中心上传头像
- 支持jpg/png/gif/webp格式
- 文件大小限制5MB
- 上传新头像后自动删除旧头像（节省存储空间）
- 更新数据库用户表的avatar字段
- 上传失败时回滚已上传文件
- 缓存同步（清除Redis中的用户信息缓存）

**技术要点**：
1. **三重文件验证**（MIME类型 + 扩展名 + 文件头魔数）
2. **事务一致性**（数据库更新失败时回滚MinIO文件）
3. **异步删除旧文件**（避免阻塞用户请求）
4. **缓存失效**（更新后立即清除Redis缓存）
5. **错误处理**（全流程异常捕获）

### 1.2 数据库表结构

```sql
CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY COMMENT '用户ID',
    `username` VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `phone` VARCHAR(20) UNIQUE COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',

    -- 头像字段（存储MinIO文件URL）
    `avatar` VARCHAR(500) COMMENT '头像URL，如: http://localhost:9000/fitness-platform/user/avatars/uuid_avatar.jpg',

    `gender` TINYINT DEFAULT 0 COMMENT '性别: 0-未知, 1-男, 2-女',
    `birth_date` DATE COMMENT '出生日期',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-正常, 1-已删除',
    INDEX idx_phone (`phone`),
    INDEX idx_status (`status`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 1.3 完整实现代码

#### 1.3.1 VO返回对象

```java
/**
 * 头像上传响应VO
 */
@Data
public class AvatarUploadVO {
    @Schema(description = "头像访问URL")
    private String avatarUrl;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "上传时间")
    private LocalDateTime uploadTime;

    @Schema(description = "状态: success/fail")
    private String status;

    @Schema(description = "提示信息")
    private String message;
}
```

#### 1.3.2 Controller层实现

```java
@RestController
@RequestMapping("/app/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final MinioUtil minioUtil;
    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Operation(summary = "上传头像", description = "上传用户头像图片，自动删除旧头像，文件大小限制5MB")
    @PostMapping("/avatar/upload")
    public Result<AvatarUploadVO> uploadAvatar(@RequestParam("file") MultipartFile file) {

        // 1. 获取当前登录用户ID（从JWT Token或ThreadLocal中解析）
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
        log.info("【头像上传】开始处理: userId={}, fileName={}, fileSize={} bytes",
                currentUserId, file.getOriginalFilename(), file.getSize());

        // ========== 第一阶段：文件验证 ==========

        // 2. 验证文件非空
        if (file == null || file.isEmpty()) {
            log.warn("【头像上传】文件为空: userId={}", currentUserId);
            return Result.fail("请选择要上传的文件");
        }

        // 3. 验证文件大小（5MB限制）
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            log.warn("【头像上传】文件过大: userId={}, fileSize={}", currentUserId, file.getSize());
            return Result.fail("文件大小不能超过5MB");
        }

        // 4. 验证文件MIME类型（第一层防护）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            log.warn("【头像上传】MIME类型非法: userId={}, contentType={}", currentUserId, contentType);
            return Result.fail("只能上传图片文件（jpg/png/gif/webp）");
        }

        // 5. 验证文件扩展名（第二层防护）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return Result.fail("文件名不合法");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

        if (!allowedExtensions.contains(extension)) {
            log.warn("【头像上传】扩展名非法: userId={}, extension={}", currentUserId, extension);
            return Result.fail("不支持的图片格式，仅支持jpg/png/gif/webp");
        }

        // 6. 验证文件头魔数（第三层防护，防止文件伪装）
        try {
            if (!validateImageHeader(file)) {
                log.warn("【头像上传】文件头魔数验证失败: userId={}", currentUserId);
                return Result.fail("图片文件校验失败，请上传真实图片");
            }
        } catch (IOException e) {
            log.error("【头像上传】文件头验证异常: userId={}", currentUserId, e);
            return Result.fail("图片文件校验异常");
        }

        // ========== 第二阶段：查询用户信息 ==========

        // 7. 查询用户当前头像URL（用于后续删除旧头像）
        User user = userService.getById(currentUserId);
        if (user == null) {
            log.error("【头像上传】用户不存在: userId={}", currentUserId);
            return Result.fail("用户不存在");
        }
        String oldAvatarUrl = user.getAvatar();
        log.info("【头像上传】查询到旧头像: userId={}, oldAvatar={}", currentUserId, oldAvatarUrl);

        // ========== 第三阶段：上传新头像到MinIO ==========

        String newAvatarUrl = null;
        try {
            // 8. 上传新头像到MinIO的 user/avatars 文件夹
            newAvatarUrl = minioUtil.uploadFile(file, "user/avatars");
            log.info("【头像上传】MinIO上传成功: userId={}, newUrl={}", currentUserId, newAvatarUrl);

        } catch (Exception e) {
            log.error("【头像上传】MinIO上传失败: userId={}", currentUserId, e);
            return Result.fail("头像上传失败，请稍后重试");
        }

        // ========== 第四阶段：更新数据库 ==========

        // 9. 更新用户头像字段
        user.setAvatar(newAvatarUrl);
        user.setUpdateTime(LocalDateTime.now());

        boolean updateSuccess = userService.updateById(user);

        if (!updateSuccess) {
            // ⚠️ 数据库更新失败，回滚：删除刚上传的MinIO文件
            log.error("【头像上传】数据库更新失败，回滚MinIO文件: userId={}, newUrl={}",
                     currentUserId, newAvatarUrl);
            try {
                minioUtil.deleteFile(newAvatarUrl);
                log.info("【头像上传】回滚成功: 已删除MinIO文件");
            } catch (Exception e) {
                log.error("【头像上传】回滚失败: 无法删除MinIO文件，需手动清理: {}", newAvatarUrl, e);
            }
            return Result.fail("头像更新失败，请稍后重试");
        }

        log.info("【头像上传】数据库更新成功: userId={}", currentUserId);

        // ========== 第五阶段：删除旧头像（异步） ==========

        // 10. 删除旧头像文件（异步处理，避免阻塞用户请求）
        if (oldAvatarUrl != null &&
            !oldAvatarUrl.isEmpty() &&
            !oldAvatarUrl.contains("default.jpg") &&  // 不删除默认头像
            !oldAvatarUrl.equals(newAvatarUrl)) {     // 不删除当前头像

            // 使用CompletableFuture异步删除，避免阻塞主线程
            CompletableFuture.runAsync(() -> {
                try {
                    minioUtil.deleteFile(oldAvatarUrl);
                    log.info("【头像上传】旧头像删除成功: userId={}, oldUrl={}", currentUserId, oldAvatarUrl);
                } catch (Exception e) {
                    // ⚠️ 旧头像删除失败不影响业务流程，仅记录日志
                    log.warn("【头像上传】旧头像删除失败（不影响业务）: userId={}, oldUrl={}",
                            currentUserId, oldAvatarUrl, e);
                }
            }, Executors.newCachedThreadPool());  // 使用线程池执行
        }

        // ========== 第六阶段：清除Redis缓存 ==========

        // 11. 清除用户信息缓存（如果使用了Redis缓存）
        String cacheKey = "user:info:" + currentUserId;
        Boolean deleted = redisTemplate.delete(cacheKey);
        if (Boolean.TRUE.equals(deleted)) {
            log.info("【头像上传】清除Redis缓存成功: key={}", cacheKey);
        }

        // ========== 第七阶段：构建响应数据 ==========

        // 12. 构建返回数据
        AvatarUploadVO vo = new AvatarUploadVO();
        vo.setAvatarUrl(newAvatarUrl);
        vo.setFileName(originalFilename);
        vo.setFileSize(file.getSize());
        vo.setUploadTime(LocalDateTime.now());
        vo.setStatus("success");
        vo.setMessage("头像上传成功");

        log.info("【头像上传】完整流程成功: userId={}, newUrl={}", currentUserId, newAvatarUrl);
        return Result.success(vo);
    }

    /**
     * 验证图片文件头（魔数）
     * 防止通过修改扩展名伪装非图片文件
     */
    private boolean validateImageHeader(MultipartFile file) throws IOException {
        byte[] header = new byte[8];
        InputStream inputStream = file.getInputStream();
        int bytesRead = inputStream.read(header);
        inputStream.close();

        if (bytesRead < 2) {
            return false;
        }

        // JPEG: FF D8 FF
        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
            log.debug("检测到JPEG文件");
            return true;
        }

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
            log.debug("检测到PNG文件");
            return true;
        }

        // GIF: 47 49 46 38
        if (header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x38) {
            log.debug("检测到GIF文件");
            return true;
        }

        // WebP: 52 49 46 46 xx xx xx xx 57 45 42 50
        if (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46 &&
            header[8 - 4] == 0x57 && header[9 - 4] == 0x45 && header[10 - 4] == 0x42 && header[11 - 4] == 0x50) {
            log.debug("检测到WebP文件");
            return true;
        }

        return false;
    }
}
```

#### 1.3.3 Service层（用户服务）

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;

    @Override
    public User getById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public boolean updateById(User user) {
        int rows = userMapper.updateById(user);
        return rows > 0;
    }

    /**
     * 根据邮箱查询用户
     */
    @Override
    public User getByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email)
               .eq(User::getIsDeleted, (byte) 0);
        return userMapper.selectOne(wrapper);
    }
}
```

#### 1.3.4 前端调用示例（Vue 3 + Element Plus）

```vue
<template>
  <div class="avatar-upload-container">
    <el-upload
      class="avatar-uploader"
      action="/app/profile/avatar/upload"
      :headers="uploadHeaders"
      :show-file-list="false"
      :on-success="handleUploadSuccess"
      :on-error="handleUploadError"
      :before-upload="beforeUpload">

      <img v-if="avatarUrl" :src="avatarUrl" class="avatar">
      <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>

      <template #tip>
        <div class="el-upload__tip">
          只能上传jpg/png/gif/webp图片，且不超过5MB
        </div>
      </template>
    </el-upload>

    <!-- 显示上传进度 -->
    <el-progress
      v-if="uploading"
      :percentage="uploadProgress"
      :stroke-width="8"
      status="success" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';

// 获取Token
const token = localStorage.getItem('token');

// 请求头（携带Token）
const uploadHeaders = computed(() => ({
  'Authorization': `Bearer ${token}`
}));

// 头像URL（从用户信息中获取）
const avatarUrl = ref('');

// 上传状态
const uploading = ref(false);
const uploadProgress = ref(0);

// 上传前校验
const beforeUpload = (file) => {
  // 1. 验证文件类型
  const isImage = /^image\/(jpeg|jpg|png|gif|webp)$/.test(file.type);
  if (!isImage) {
    ElMessage.error('只能上传jpg/png/gif/webp图片!');
    return false;
  }

  // 2. 验证文件大小
  const isLt5M = file.size / 1024 / 1024 < 5;
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过5MB!');
    return false;
  }

  // 3. 显示上传进度
  uploading.value = true;
  uploadProgress.value = 0;

  // 模拟上传进度（实际应使用onProgress回调）
  const timer = setInterval(() => {
    if (uploadProgress.value < 90) {
      uploadProgress.value += 10;
    } else {
      clearInterval(timer);
    }
  }, 200);

  return true;
};

// 上传成功回调
const handleUploadSuccess = (response) => {
  uploading.value = false;
  uploadProgress.value = 100;

  if (response.code === 200) {
    avatarUrl.value = response.data.avatarUrl;
    ElMessage.success('头像上传成功!');

    // 更新本地存储的用户信息
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
    userInfo.avatar = response.data.avatarUrl;
    localStorage.setItem('userInfo', JSON.stringify(userInfo));

  } else {
    ElMessage.error(response.message || '头像上传失败');
  }

  // 2秒后隐藏进度条
  setTimeout(() => {
    uploadProgress.value = 0;
  }, 2000);
};

// 上传失败回调
const handleUploadError = (error) => {
  uploading.value = false;
  uploadProgress.value = 0;

  console.error('头像上传失败:', error);
  ElMessage.error('头像上传失败，请重试!');
};
</script>

<style scoped>
.avatar-upload-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-uploader .avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  display: block;
  object-fit: cover;
  border: 2px solid #dcdfe6;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  line-height: 120px;
  text-align: center;
  border: 2px dashed #d9d9d9;
  border-radius: 50%;
  cursor: pointer;
  transition: border-color 0.3s;
}

.avatar-uploader-icon:hover {
  border-color: #409eff;
}

.el-upload__tip {
  margin-top: 10px;
  color: #606266;
  font-size: 12px;
}
</style>
```

### 1.4 逻辑合理性分析

#### ✅ 1.4.1 优点

| 方面 | 实现方式 | 合理性 |
|------|---------|--------|
| **文件验证** | 三重验证（MIME + 扩展名 + 文件头） | ✅ 防止文件伪装攻击 |
| **事务一致性** | 数据库更新失败时回滚MinIO文件 | ✅ 保证数据一致性 |
| **异步删除** | CompletableFuture异步删除旧文件 | ✅ 不阻塞用户请求 |
| **缓存同步** | 更新后立即清除Redis缓存 | ✅ 防止脏读 |
| **日志记录** | 每个阶段详细日志 | ✅ 便于问题排查 |
| **错误处理** | 全流程异常捕获 | ✅ 提升系统健壮性 |
| **用户体验** | 前端实时显示上传进度 | ✅ 增强交互性 |

#### ⚠️ 1.4.2 潜在问题与优化建议

| 问题 | 影响 | 优化方案 |
|------|------|---------|
| **大文件上传慢** | 用户等待时间长 | 1. 前端压缩图片（如使用compressorjs）<br>2. 后端集成Thumbnailator压缩<br>3. 使用分片上传 |
| **并发上传** | 重复上传覆盖 | 添加分布式锁：`lock:avatar:upload:{userId}` |
| **文件头验证开销** | 读取文件流增加耗时 | 1. 仅在必要时验证（如怀疑攻击）<br>2. 使用缓存验证结果 |
| **旧文件删除失败** | MinIO垃圾文件堆积 | 1. 记录删除失败的文件到数据库表<br>2. 定时任务清理（如每周一次） |
| **MinIO单点故障** | 上传功能不可用 | 1. MinIO集群部署<br>2. 降级方案：切换到本地文件系统 |

#### 🚀 1.4.3 生产环境增强方案

**方案1：图片压缩（减少存储成本）**

添加依赖：
```xml
<dependency>
    <groupId>net.coobird</groupId>
    <artifactId>thumbnailator</artifactId>
    <version>0.4.19</version>
</dependency>
```

压缩工具类：
```java
@Component
public class ImageCompressor {

    /**
     * 压缩图片（如果超过阈值）
     * @param file 原始文件
     * @return 压缩后的字节数组
     */
    public byte[] compressIfNeeded(MultipartFile file) throws IOException {
        BufferedImage image = ImageIO.read(file.getInputStream());

        if (image == null) {
            throw new IOException("无法读取图片文件");
        }

        // 如果图片尺寸过大，压缩到800x800（保持比例）
        if (image.getWidth() > 800 || image.getHeight() > 800) {
            log.info("图片过大，开始压缩: 原始尺寸={}x{}", image.getWidth(), image.getHeight());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Thumbnails.of(image)
                    .size(800, 800)              // 最大尺寸800x800
                    .outputQuality(0.8)          // 质量80%
                    .outputFormat("jpg")         // 统一转为jpg
                    .toOutputStream(baos);

            byte[] compressed = baos.toByteArray();
            log.info("压缩完成: 原始大小={}, 压缩后={}, 压缩率={}%",
                    file.getSize(), compressed.length,
                    (100 - compressed.length * 100 / file.getSize()));

            return compressed;
        }

        return file.getBytes();
    }
}
```

在Controller中使用：
```java
@Autowired
private ImageCompressor imageCompressor;

// 在上传前压缩
byte[] compressedData = imageCompressor.compressIfNeeded(file);
MultipartFile compressedFile = new MockMultipartFile(
    file.getName(),
    file.getOriginalFilename(),
    "image/jpeg",
    compressedData
);
String avatarUrl = minioUtil.uploadFile(compressedFile, "user/avatars");
```

**方案2：并发上传控制（防止重复上传）**

```java
@RestController
@RequestMapping("/app/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final RedisTemplate<String, String> redisTemplate;

    @PostMapping("/avatar/upload")
    public Result<AvatarUploadVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        String lockKey = "lock:avatar:upload:" + userId;

        // 尝试获取分布式锁（30秒超时）
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 30, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(locked)) {
            return Result.fail("上传中，请勿重复提交");
        }

        try {
            // 执行上传逻辑...
            return uploadAvatarInternal(file, userId);
        } finally {
            // 释放锁
            redisTemplate.delete(lockKey);
        }
    }

    private Result<AvatarUploadVO> uploadAvatarInternal(MultipartFile file, Long userId) {
        // 原有上传逻辑
        // ...
    }
}
```

**方案3：垃圾文件定时清理**

创建清理任务表：
```sql
CREATE TABLE `orphan_file_cleanup` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `file_url` VARCHAR(500) NOT NULL COMMENT '孤儿文件URL',
    `reason` VARCHAR(200) COMMENT '原因: 回滚失败/删除失败',
    `retry_count` INT DEFAULT 0 COMMENT '重试次数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `status` TINYINT DEFAULT 0 COMMENT '0-待清理, 1-已清理, 2-清理失败',
    INDEX idx_status (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='孤儿文件清理任务表';
```

定时任务清理：
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OrphanFileCleanupTask {

    private final OrphanFileCleanupMapper cleanupMapper;
    private final MinioUtil minioUtil;

    /**
     * 每周日凌晨3点执行清理任务
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void cleanupOrphanFiles() {
        log.info("【定时任务】开始清理孤儿文件");

        // 查询待清理的文件（状态=0且重试次数<3）
        List<OrphanFileCleanup> files = cleanupMapper.selectPendingFiles();

        int successCount = 0;
        int failCount = 0;

        for (OrphanFileCleanup file : files) {
            try {
                minioUtil.deleteFile(file.getFileUrl());

                // 更新状态为已清理
                file.setStatus((byte) 1);
                cleanupMapper.updateById(file);

                successCount++;
                log.info("【定时任务】孤儿文件清理成功: id={}, url={}", file.getId(), file.getFileUrl());

            } catch (Exception e) {
                // 增加重试次数
                file.setRetryCount(file.getRetryCount() + 1);

                if (file.getRetryCount() >= 3) {
                    file.setStatus((byte) 2);  // 标记为清理失败
                }

                cleanupMapper.updateById(file);
                failCount++;

                log.error("【定时任务】孤儿文件清理失败: id={}, url={}, retryCount={}",
                         file.getId(), file.getFileUrl(), file.getRetryCount(), e);
            }
        }

        log.info("【定时任务】孤儿文件清理完成: 成功={}, 失败={}", successCount, failCount);
    }
}
```

在Controller的回滚逻辑中记录失败文件：
```java
// 数据库更新失败，回滚MinIO文件
try {
    minioUtil.deleteFile(newAvatarUrl);
} catch (Exception e) {
    // 记录到清理任务表
    OrphanFileCleanup cleanup = new OrphanFileCleanup();
    cleanup.setFileUrl(newAvatarUrl);
    cleanup.setReason("数据库更新失败，回滚删除MinIO文件失败");
    cleanup.setRetryCount(0);
    cleanup.setStatus((byte) 0);
    cleanupMapper.insert(cleanup);

    log.error("【头像上传】回滚失败，已记录到清理任务表: {}", newAvatarUrl, e);
}
```

---

## 二、外部API服务集成超详细指南

### 2.1 高德地图API深度集成

#### 2.1.1 API能力全景图

```
高德地图API能力矩阵
├── 地理编码服务
│   ├── 地理编码（地址→坐标）
│   ├── 逆地理编码（坐标→地址）
│   └── 批量地理编码
├── 路径规划服务
│   ├── 驾车路径规划
│   ├── 步行路径规划
│   ├── 骑行路径规划
│   ├── 货车路径规划
│   └── 公交路径规划
├── 距离测量服务
│   ├── 直线距离
│   ├── 驾车距离
│   └── 批量距离测量
├── 搜索服务
│   ├── 关键词搜索（POI）
│   ├── 周边搜索
│   ├── 多边形区域搜索
│   └── ID查询
├── 静态地图服务
│   └── 生成地图图片
└── 天气查询服务
    ├── 实时天气
    ├── 天气预报
    └── 生活指数
```

#### 2.1.2 API 1: 地理编码（地址→坐标）

**场景说明**：
- Admin端新增门店时，输入地址自动获取经纬度
- 用户输入配送地址时，自动获取坐标用于配送范围判断

**API文档**：https://lbs.amap.com/api/webservice/guide/api/georegeo

**请求示例**：
```http
GET https://restapi.amap.com/v3/geocode/geo
  ?key=YOUR_API_KEY
  &address=北京市朝阳区望京SOHO T3
  &city=北京市
```

**参数说明**：

| 参数 | 必填 | 类型 | 说明 |
|------|------|------|------|
| key | ✅ | String | 高德API Key |
| address | ✅ | String | 地址（支持模糊查询） |
| city | ❌ | String | 城市名称（提高准确度） |
| batch | ❌ | Boolean | 是否批量查询 |
| sig | ❌ | String | 签名（数字签名认证） |

**响应示例**：
```json
{
  "status": "1",
  "info": "OK",
  "infocode": "10000",
  "count": "1",
  "geocodes": [{
    "formatted_address": "北京市朝阳区望京街道望京SOHO T3",
    "country": "中国",
    "province": "北京市",
    "citycode": "010",
    "city": "北京市",
    "district": "朝阳区",
    "township": "望京街道",
    "street": "阜通东大街",
    "number": "6号",
    "location": "116.481488,39.996893",  // 经度,纬度
    "level": "门牌号"
  }]
}
```

**Java集成代码**：
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AmapGeocodeService {

    private final AmapProperties amapProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 地理编码：地址转坐标
     * @param address 详细地址
     * @param city 城市名称（可选，提高准确度）
     * @return {latitude: 39.996893, longitude: 116.481488, formattedAddress: "详细地址"}
     */
    public GeocodeResult getLocationByAddress(String address, String city) {
        // 1. 参数校验
        if (!StringUtils.hasText(address)) {
            throw new BusinessException("地址不能为空");
        }

        // 2. 检查Redis缓存（减少API调用）
        String cacheKey = "amap:geocode:" + address + ":" + (city != null ? city : "");
        GeocodeResult cached = (GeocodeResult) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("【高德地理编码】命中缓存: address={}", address);
            return cached;
        }

        try {
            // 3. 构建请求URL
            String url = amapProperties.getWebServiceUrl() +
                        amapProperties.getGeocodeUrl() +
                        "?key=" + amapProperties.getApiKey() +
                        "&address=" + URLEncoder.encode(address, StandardCharsets.UTF_8);

            if (StringUtils.hasText(city)) {
                url += "&city=" + URLEncoder.encode(city, StandardCharsets.UTF_8);
            }

            log.info("【高德地理编码】请求: address={}, city={}", address, city);

            // 4. 调用高德API
            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSONObject.parseObject(response);

            // 5. 解析响应
            if ("1".equals(json.getString("status"))) {
                Integer count = json.getInteger("count");
                if (count == null || count == 0) {
                    throw new BusinessException("未找到该地址的坐标信息，请检查地址是否正确");
                }

                JSONObject geocode = json.getJSONArray("geocodes").getJSONObject(0);
                String location = geocode.getString("location");
                String[] coords = location.split(",");

                GeocodeResult result = new GeocodeResult();
                result.setLongitude(Double.parseDouble(coords[0]));
                result.setLatitude(Double.parseDouble(coords[1]));
                result.setFormattedAddress(geocode.getString("formatted_address"));
                result.setProvince(geocode.getString("province"));
                result.setCity(geocode.getString("city"));
                result.setDistrict(geocode.getString("district"));
                result.setLevel(geocode.getString("level"));

                // 6. 写入Redis缓存（24小时过期）
                redisTemplate.opsForValue().set(cacheKey, result, 24, TimeUnit.HOURS);

                log.info("【高德地理编码】成功: address={}, location={},{}",
                        address, result.getLongitude(), result.getLatitude());

                return result;
            } else {
                String info = json.getString("info");
                log.error("【高德地理编码】失败: address={}, info={}", address, info);
                throw new BusinessException("地理编码失败: " + info);
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("【高德地理编码】异常: address={}, city={}", address, city, e);
            throw new BusinessException("地理编码服务异常，请稍后重试");
        }
    }
}

/**
 * 地理编码结果VO
 */
@Data
public class GeocodeResult {
    private Double longitude;        // 经度
    private Double latitude;         // 纬度
    private String formattedAddress; // 格式化地址
    private String province;         // 省份
    private String city;             // 城市
    private String district;         // 区县
    private String level;            // 匹配级别: 门牌号/小区/道路/区县/城市
}
```

**Controller使用**：
```java
@RestController
@RequestMapping("/admin/gym-store")
@RequiredArgsConstructor
public class GymStoreController {

    private final AmapGeocodeService geocodeService;

    @Operation(summary = "地址转坐标", description = "输入地址自动获取经纬度")
    @GetMapping("/geocode")
    public Result<GeocodeResult> geocode(
            @RequestParam String address,
            @RequestParam(required = false) String city) {

        GeocodeResult result = geocodeService.getLocationByAddress(address, city);
        return Result.success(result);
    }
}
```

**前端集成（Element Plus）**：
```vue
<template>
  <el-form :model="formData" label-width="120px">
    <!-- 省市区级联选择器 -->
    <el-form-item label="所在城市">
      <el-cascader
        v-model="formData.cityCode"
        :options="cityOptions"
        placeholder="请选择省/市/区"
        @change="handleCityChange" />
    </el-form-item>

    <!-- 详细地址输入 -->
    <el-form-item label="详细地址">
      <el-input
        v-model="formData.address"
        placeholder="请输入详细地址（如：望京SOHO T3 A座）"
        @blur="handleAddressBlur">
        <template #append>
          <el-button @click="getCoordinates" :loading="geocoding">
            获取坐标
          </el-button>
        </template>
      </el-input>
    </el-form-item>

    <!-- 自动填充的经纬度 -->
    <el-form-item label="经度">
      <el-input v-model="formData.longitude" readonly placeholder="自动获取" />
    </el-form-item>

    <el-form-item label="纬度">
      <el-input v-model="formData.latitude" readonly placeholder="自动获取" />
    </el-form-item>

    <!-- 格式化地址展示 -->
    <el-form-item label="完整地址" v-if="formData.formattedAddress">
      <el-tag>{{ formData.formattedAddress }}</el-tag>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import axios from 'axios';

const formData = reactive({
  cityCode: [],
  city: '',
  address: '',
  longitude: '',
  latitude: '',
  formattedAddress: ''
});

const geocoding = ref(false);

// 城市选择变化
const handleCityChange = (value) => {
  // value: ['11', '1101', '110105'] (北京/市辖区/朝阳区)
  formData.city = getCityName(value);  // 获取城市名称
};

// 地址输入框失焦时自动获取坐标
const handleAddressBlur = () => {
  if (formData.address && formData.city) {
    getCoordinates();
  }
};

// 获取坐标
const getCoordinates = async () => {
  if (!formData.address) {
    ElMessage.warning('请输入详细地址');
    return;
  }

  geocoding.value = true;

  try {
    const response = await axios.get('/admin/gym-store/geocode', {
      params: {
        address: formData.address,
        city: formData.city
      }
    });

    if (response.data.code === 200) {
      const result = response.data.data;

      formData.longitude = result.longitude;
      formData.latitude = result.latitude;
      formData.formattedAddress = result.formattedAddress;

      ElMessage.success({
        message: '坐标获取成功',
        duration: 2000
      });
    } else {
      ElMessage.error(response.data.message);
    }
  } catch (error) {
    console.error('地理编码失败:', error);
    ElMessage.error('地理编码失败，请重试');
  } finally {
    geocoding.value = false;
  }
};
</script>
```

---

#### 2.1.3 API 2: 距离测量（实际路径距离）

**场景说明**：
- 用户查看门店详情时，显示驾车/步行到达距离和耗时
- 计算配送范围（判断用户地址是否在配送半径内）

**API文档**：https://lbs.amap.com/api/webservice/guide/api/distance

**请求示例**：
```http
GET https://restapi.amap.com/v3/distance
  ?key=YOUR_API_KEY
  &origins=116.481488,39.996893        # 起点坐标（用户位置）
  &destination=116.434446,39.90816     # 终点坐标（门店位置）
  &type=1                              # 1=驾车距离, 0=直线距离
```

**参数说明**：

| 参数 | 必填 | 类型 | 说明 |
|------|------|------|------|
| key | ✅ | String | 高德API Key |
| origins | ✅ | String | 起点坐标（经度,纬度） |
| destination | ✅ | String | 终点坐标（经度,纬度） |
| type | ✅ | Integer | 0=直线距离, 1=驾车距离 |
| output | ❌ | String | 返回格式: json/xml，默认json |

**响应示例**：
```json
{
  "status": "1",
  "info": "OK",
  "infocode": "10000",
  "count": "1",
  "results": [{
    "origin_id": "1",
    "dest_id": "1",
    "distance": "12580",    // 实际路径距离（米）
    "duration": "1350"      // 预计耗时（秒）
  }]
}
```

**Java集成代码**：
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AmapDistanceService {

    private final AmapProperties amapProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 计算实际路径距离
     * @param originLon 起点经度
     * @param originLat 起点纬度
     * @param destLon 终点经度
     * @param destLat 终点纬度
     * @param type 1=驾车距离, 0=直线距离
     * @return {distance: 12.58(公里), duration: 22(分钟), drivingText: "约12.6公里，22分钟"}
     */
    public DistanceResult calculateDistance(
            Double originLon, Double originLat,
            Double destLon, Double destLat,
            Integer type) {

        // 1. 参数校验
        if (originLon == null || originLat == null || destLon == null || destLat == null) {
            throw new BusinessException("坐标参数不能为空");
        }

        try {
            // 2. 构建请求URL
            String url = amapProperties.getWebServiceUrl() +
                        amapProperties.getDistanceUrl() +
                        "?key=" + amapProperties.getApiKey() +
                        "&origins=" + originLon + "," + originLat +
                        "&destination=" + destLon + "," + destLat +
                        "&type=" + type;

            log.info("【高德距离测量】请求: from=({},{}), to=({},{}), type={}",
                    originLon, originLat, destLon, destLat, type);

            // 3. 调用高德API
            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSONObject.parseObject(response);

            // 4. 解析响应
            if ("1".equals(json.getString("status"))) {
                JSONObject result = json.getJSONArray("results").getJSONObject(0);

                double distanceInKm = result.getDouble("distance") / 1000;  // 米 → 公里
                int durationInMin = result.getInteger("duration") / 60;      // 秒 → 分钟

                DistanceResult distanceResult = new DistanceResult();
                distanceResult.setDistance(Math.round(distanceInKm * 100.0) / 100.0);  // 保留2位小数
                distanceResult.setDuration(durationInMin);
                distanceResult.setType(type == 1 ? "driving" : "straight");
                distanceResult.setDrivingText(String.format("约%.1f公里，%d分钟", distanceInKm, durationInMin));

                log.info("【高德距离测量】成功: distance={}km, duration={}min",
                        distanceResult.getDistance(), distanceResult.getDuration());

                return distanceResult;

            } else {
                String info = json.getString("info");
                log.error("【高德距离测量】失败: info={}", info);
                throw new BusinessException("距离测量失败: " + info);
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("【高德距离测量】异常", e);
            throw new BusinessException("距离测量服务异常，请稍后重试");
        }
    }

    /**
     * 批量计算距离（一个起点到多个终点）
     * @param originLon 起点经度
     * @param originLat 起点纬度
     * @param destinations 终点列表 [{lon, lat, id}]
     * @param type 1=驾车, 0=直线
     * @return Map<终点ID, DistanceResult>
     */
    public Map<String, DistanceResult> batchCalculateDistance(
            Double originLon, Double originLat,
            List<Destination> destinations,
            Integer type) {

        // 高德API批量查询最多支持100个目的地
        if (destinations.size() > 100) {
            throw new BusinessException("批量查询最多支持100个目的地");
        }

        try {
            // 构建目的地坐标字符串: "lon1,lat1|lon2,lat2|lon3,lat3"
            String destStr = destinations.stream()
                    .map(d -> d.getLongitude() + "," + d.getLatitude())
                    .collect(Collectors.joining("|"));

            String url = amapProperties.getWebServiceUrl() +
                        amapProperties.getDistanceUrl() +
                        "?key=" + amapProperties.getApiKey() +
                        "&origins=" + originLon + "," + originLat +
                        "&destination=" + destStr +
                        "&type=" + type;

            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSONObject.parseObject(response);

            if ("1".equals(json.getString("status"))) {
                JSONArray results = json.getJSONArray("results");

                Map<String, DistanceResult> resultMap = new HashMap<>();

                for (int i = 0; i < results.size(); i++) {
                    JSONObject result = results.getJSONObject(i);
                    Destination dest = destinations.get(i);

                    double distanceInKm = result.getDouble("distance") / 1000;
                    int durationInMin = result.getInteger("duration") / 60;

                    DistanceResult distanceResult = new DistanceResult();
                    distanceResult.setDistance(Math.round(distanceInKm * 100.0) / 100.0);
                    distanceResult.setDuration(durationInMin);
                    distanceResult.setType(type == 1 ? "driving" : "straight");

                    resultMap.put(dest.getId(), distanceResult);
                }

                log.info("【高德批量距离测量】成功: 起点=({},{}), 目的地数量={}",
                        originLon, originLat, destinations.size());

                return resultMap;
            } else {
                throw new BusinessException("批量距离测量失败: " + json.getString("info"));
            }

        } catch (Exception e) {
            log.error("【高德批量距离测量】异常", e);
            throw new BusinessException("批量距离测量服务异常");
        }
    }
}

/**
 * 距离测量结果VO
 */
@Data
public class DistanceResult {
    private Double distance;      // 距离（公里）
    private Integer duration;     // 耗时（分钟）
    private String type;          // 类型: driving/straight
    private String drivingText;   // 友好文本: "约12.6公里，22分钟"
}

/**
 * 目的地坐标
 */
@Data
@AllArgsConstructor
public class Destination {
    private String id;          // 目的地ID（如门店ID）
    private Double longitude;   // 经度
    private Double latitude;    // 纬度
}
```

**Controller使用**（门店详情显示实际距离）：
```java
@RestController
@RequestMapping("/app/gym-store")
@RequiredArgsConstructor
public class AppGymStoreController {

    private final AppGymStoreService appGymStoreService;
    private final AmapDistanceService distanceService;

    @Operation(summary = "查询门店详情", description = "支持显示实际驾车/步行距离")
    @GetMapping("/{id}")
    public Result<GymStoreDetailVO> detail(
            @PathVariable Long id,
            @RequestParam(required = false) Double userLatitude,
            @RequestParam(required = false) Double userLongitude,
            @RequestParam(defaultValue = "0") Integer distanceType) {  // 0=直线(Haversine), 1=驾车, 2=步行

        // 1. 查询门店基本信息
        GymStoreDetailVO detailVO = appGymStoreService.getStoreDetail(id);

        // 2. 如果用户提供了位置，计算距离
        if (userLatitude != null && userLongitude != null) {

            if (distanceType == 0) {
                // 使用Haversine公式计算直线距离（性能最好）
                Double distance = appGymStoreService.calculateHaversineDistance(
                    userLatitude, userLongitude,
                    detailVO.getLatitude(), detailVO.getLongitude()
                );
                detailVO.setDistance(distance);
                detailVO.setDistanceText(String.format("直线距离约%.1f公里", distance));

            } else {
                // 调用高德API计算实际路径距离
                DistanceResult distanceResult = distanceService.calculateDistance(
                    userLongitude, userLatitude,
                    detailVO.getLongitude(), detailVO.getLatitude(),
                    distanceType  // 1=驾车, 2=步行
                );

                detailVO.setDistance(distanceResult.getDistance());
                detailVO.setDuration(distanceResult.getDuration());
                detailVO.setDistanceText(distanceResult.getDrivingText());
            }
        }

        return Result.success(detailVO);
    }
}
```

**对比Haversine直线距离与高德实际距离**：

```java
/**
 * 距离对比测试
 */
@Test
public void testDistanceComparison() {
    // 起点: 天安门广场
    Double originLon = 116.3972;
    Double originLat = 39.9075;

    // 终点: 望京SOHO
    Double destLon = 116.481488;
    Double destLat = 39.996893;

    // 方案1: Haversine直线距离（项目已实现）
    Double haversineDistance = appGymStoreService.calculateHaversineDistance(
        originLat, originLon, destLat, destLon
    );
    System.out.println("Haversine直线距离: " + haversineDistance + " 公里");

    // 方案2: 高德直线距离
    DistanceResult amapStraight = distanceService.calculateDistance(
        originLon, originLat, destLon, destLat, 0
    );
    System.out.println("高德直线距离: " + amapStraight.getDistance() + " 公里");

    // 方案3: 高德驾车距离
    DistanceResult amapDriving = distanceService.calculateDistance(
        originLon, originLat, destLon, destLat, 1
    );
    System.out.println("高德驾车距离: " + amapDriving.getDistance() + " 公里, 耗时: " + amapDriving.getDuration() + " 分钟");

    /*
     * 预期输出（示例）:
     * Haversine直线距离: 13.2 公里
     * 高德直线距离: 13.18 公里
     * 高德驾车距离: 16.5 公里, 耗时: 28 分钟
     *
     * 结论: Haversine与高德直线距离误差<1%，性能更好，推荐日常使用
     *       高德驾车距离更准确，适合导航场景，但消耗API配额
     */
}
```

---

#### 2.1.4 API 3: 路径规划（导航）

**场景说明**：
- 用户点击"导航到门店"，获取详细导航路线
- 显示途经道路、转向提示、预计时间等

**API文档**：
- 步行：https://lbs.amap.com/api/webservice/guide/api/direction#walking
- 驾车：https://lbs.amap.com/api/webservice/guide/api/direction#driving

**请求示例**（步行路径）：
```http
GET https://restapi.amap.com/v3/direction/walking
  ?key=YOUR_API_KEY
  &origin=116.481488,39.996893    # 起点坐标
  &destination=116.434446,39.90816 # 终点坐标
```

**响应示例**：
```json
{
  "status": "1",
  "info": "OK",
  "route": {
    "origin": "116.481488,39.996893",
    "destination": "116.434446,39.90816",
    "paths": [{
      "distance": "11520",        // 总距离（米）
      "duration": "8280",         // 总耗时（秒）
      "steps": [
        {
          "instruction": "向西步行120米左转",
          "orientation": "西",
          "road": "阜通东大街",
          "distance": "120",
          "duration": "86",
          "polyline": "116.481488,39.996893;116.480000,39.996893",  // 路径坐标点
          "action": "左转",
          "assistant_action": ""
        },
        {
          "instruction": "沿阜通东大街向北步行500米",
          "orientation": "北",
          "road": "阜通东大街",
          "distance": "500",
          "duration": "360",
          "polyline": "116.480000,39.996893;116.480000,40.001893",
          "action": "直行",
          "assistant_action": ""
        }
        // ... 更多步骤
      ]
    }]
  }
}
```

**Java集成代码**：
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AmapDirectionService {

    private final AmapProperties amapProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 路径规划
     * @param originLon 起点经度
     * @param originLat 起点纬度
     * @param destLon 终点经度
     * @param destLat 终点纬度
     * @param type 1=步行, 2=驾车
     * @return 路径详情
     */
    public DirectionResult getRoute(
            Double originLon, Double originLat,
            Double destLon, Double destLat,
            Integer type) {

        try {
            // 选择API端点
            String apiUrl = type == 1 ?
                    amapProperties.getDirectionWalkingUrl() :
                    amapProperties.getDirectionDrivingUrl();

            String url = amapProperties.getWebServiceUrl() + apiUrl +
                        "?key=" + amapProperties.getApiKey() +
                        "&origin=" + originLon + "," + originLat +
                        "&destination=" + destLon + "," + destLat;

            log.info("【高德路径规划】请求: from=({},{}), to=({},{}), type={}",
                    originLon, originLat, destLon, destLat, type == 1 ? "步行" : "驾车");

            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSONObject.parseObject(response);

            if ("1".equals(json.getString("status"))) {
                JSONObject route = json.getJSONObject("route");
                JSONObject path = route.getJSONArray("paths").getJSONObject(0);

                DirectionResult result = new DirectionResult();
                result.setDistance(path.getDouble("distance") / 1000);  // 米 → 公里
                result.setDuration(path.getInteger("duration") / 60);    // 秒 → 分钟
                result.setType(type == 1 ? "walking" : "driving");

                // 解析导航步骤
                JSONArray stepsArray = path.getJSONArray("steps");
                List<NavigationStep> steps = new ArrayList<>();

                for (int i = 0; i < stepsArray.size(); i++) {
                    JSONObject stepJson = stepsArray.getJSONObject(i);

                    NavigationStep step = new NavigationStep();
                    step.setStepIndex(i + 1);
                    step.setInstruction(stepJson.getString("instruction"));
                    step.setRoad(stepJson.getString("road"));
                    step.setOrientation(stepJson.getString("orientation"));
                    step.setDistance(stepJson.getDouble("distance"));
                    step.setDuration(stepJson.getInteger("duration") / 60);
                    step.setAction(stepJson.getString("action"));
                    step.setPolyline(stepJson.getString("polyline"));

                    steps.add(step);
                }

                result.setSteps(steps);

                log.info("【高德路径规划】成功: 总距离={}km, 总耗时={}min, 步骤数={}",
                        result.getDistance(), result.getDuration(), steps.size());

                return result;

            } else {
                throw new BusinessException("路径规划失败: " + json.getString("info"));
            }

        } catch (Exception e) {
            log.error("【高德路径规划】异常", e);
            throw new BusinessException("路径规划服务异常");
        }
    }
}

/**
 * 路径规划结果VO
 */
@Data
public class DirectionResult {
    private Double distance;              // 总距离（公里）
    private Integer duration;             // 总耗时（分钟）
    private String type;                  // 类型: walking/driving
    private List<NavigationStep> steps;   // 导航步骤
}

/**
 * 导航步骤
 */
@Data
public class NavigationStep {
    private Integer stepIndex;      // 步骤序号
    private String instruction;     // 导航指令（如："向西步行120米左转"）
    private String road;            // 道路名称
    private String orientation;     // 方向（东/西/南/北）
    private Double distance;        // 步骤距离（米）
    private Integer duration;       // 步骤耗时（分钟）
    private String action;          // 动作（左转/右转/直行）
    private String polyline;        // 路径坐标点（用于地图绘制）
}
```

**Controller使用**（获取导航路线）：
```java
@RestController
@RequestMapping("/app/gym-store")
@RequiredArgsConstructor
public class AppGymStoreController {

    private final AmapDirectionService directionService;

    @Operation(summary = "获取导航路线", description = "获取从用户位置到门店的导航路线")
    @GetMapping("/{id}/navigation")
    public Result<DirectionResult> getNavigation(
            @PathVariable Long id,
            @RequestParam Double userLatitude,
            @RequestParam Double userLongitude,
            @RequestParam(defaultValue = "1") Integer type) {  // 1=步行, 2=驾车

        // 1. 查询门店坐标
        GymStore store = gymStoreService.getById(id);
        if (store == null) {
            return Result.fail("门店不存在");
        }

        // 2. 调用高德API获取路径
        DirectionResult route = directionService.getRoute(
            userLongitude, userLatitude,
            store.getLongitude(), store.getLatitude(),
            type
        );

        return Result.success(route);
    }
}
```

**前端展示导航步骤**：
```vue
<template>
  <div class="navigation-panel">
    <div class="nav-header">
      <h3>{{ route.type === 'walking' ? '步行' : '驾车' }}导航</h3>
      <p class="nav-summary">
        全程约{{ route.distance }}公里，预计{{ route.duration }}分钟
      </p>
    </div>

    <div class="nav-steps">
      <div
        v-for="step in route.steps"
        :key="step.stepIndex"
        class="nav-step">

        <div class="step-index">{{ step.stepIndex }}</div>

        <div class="step-content">
          <div class="step-instruction">{{ step.instruction }}</div>
          <div class="step-detail">
            <span class="step-road">{{ step.road }}</span>
            <span class="step-distance">{{ step.distance }}米</span>
          </div>
        </div>

        <div class="step-icon">
          <i :class="getActionIcon(step.action)"></i>
        </div>
      </div>
    </div>

    <!-- 唤起高德地图APP导航 -->
    <el-button type="primary" @click="launchAmapApp" size="large" class="launch-btn">
      <i class="icon-map"></i> 打开高德地图导航
    </el-button>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  route: Object,
  storeName: String,
  storeLat: Number,
  storeLon: Number
});

// 根据动作返回图标类名
const getActionIcon = (action) => {
  const iconMap = {
    '左转': 'el-icon-turn-left',
    '右转': 'el-icon-turn-right',
    '直行': 'el-icon-top',
    '到达': 'el-icon-location'
  };
  return iconMap[action] || 'el-icon-right';
};

// 唤起高德地图APP
const launchAmapApp = () => {
  const userAgent = navigator.userAgent;

  // iOS设备
  if (/(iPhone|iPad|iPod)/i.test(userAgent)) {
    window.location.href = `iosamap://path?sourceApplication=健身平台&dlat=${props.storeLat}&dlon=${props.storeLon}&dname=${props.storeName}&dev=0&t=0`;
  }
  // Android设备
  else if (/Android/i.test(userAgent)) {
    window.location.href = `androidamap://route?sourceApplication=健身平台&dlat=${props.storeLat}&dlon=${props.storeLon}&dname=${props.storeName}&dev=0&t=0`;
  }
  // PC端（打开高德地图网页版）
  else {
    window.open(`https://uri.amap.com/navigation?to=${props.storeLon},${props.storeLat},${props.storeName}&mode=walk&src=健身平台`);
  }
};
</script>

<style scoped>
.navigation-panel {
  padding: 20px;
}

.nav-header {
  border-bottom: 1px solid #eee;
  padding-bottom: 15px;
  margin-bottom: 20px;
}

.nav-summary {
  color: #666;
  font-size: 14px;
  margin-top: 5px;
}

.nav-steps {
  max-height: 400px;
  overflow-y: auto;
}

.nav-step {
  display: flex;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.step-index {
  width: 30px;
  height: 30px;
  background: #409eff;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-weight: bold;
}

.step-content {
  flex: 1;
  padding: 0 15px;
}

.step-instruction {
  font-size: 15px;
  color: #303133;
  margin-bottom: 5px;
}

.step-detail {
  font-size: 13px;
  color: #909399;
}

.step-road {
  margin-right: 10px;
}

.step-icon {
  color: #409eff;
  font-size: 20px;
}

.launch-btn {
  width: 100%;
  margin-top: 20px;
}
</style>
```

**唤起高德地图APP导航**（URL Scheme）：

```javascript
/**
 * 唤起高德地图APP导航
 * @param {Number} destLat - 目的地纬度
 * @param {Number} destLon - 目的地经度
 * @param {String} destName - 目的地名称
 * @param {String} mode - 导航模式: walk/drive/bus
 */
function launchAmapNavigation(destLat, destLon, destName, mode = 'walk') {
  const userAgent = navigator.userAgent;

  // iOS设备
  if (/(iPhone|iPad|iPod)/i.test(userAgent)) {
    const scheme = `iosamap://path?` +
                   `sourceApplication=健身平台` +
                   `&dlat=${destLat}` +
                   `&dlon=${destLon}` +
                   `&dname=${encodeURIComponent(destName)}` +
                   `&dev=0` +  // 使用GPS定位
                   `&t=${getModeCode(mode)}`;  // 0=驾车, 1=公交, 2=步行

    window.location.href = scheme;

    // 如果5秒后没有成功唤起（说明未安装高德地图），跳转到App Store
    setTimeout(() => {
      window.location.href = 'https://apps.apple.com/cn/app/id461703208';
    }, 5000);
  }
  // Android设备
  else if (/Android/i.test(userAgent)) {
    const scheme = `androidamap://route?` +
                   `sourceApplication=健身平台` +
                   `&dlat=${destLat}` +
                   `&dlon=${destLon}` +
                   `&dname=${encodeURIComponent(destName)}` +
                   `&dev=0` +
                   `&t=${getModeCode(mode)}`;

    window.location.href = scheme;

    // 如果5秒后没有成功唤起，跳转到应用宝
    setTimeout(() => {
      window.location.href = 'https://a.app.qq.com/o/simple.jsp?pkgname=com.autonavi.minimap';
    }, 5000);
  }
  // PC端（打开高德地图网页版）
  else {
    const webUrl = `https://uri.amap.com/navigation?` +
                   `to=${destLon},${destLat},${encodeURIComponent(destName)}` +
                   `&mode=${mode}` +
                   `&src=健身平台`;

    window.open(webUrl, '_blank');
  }
}

// 获取导航模式代码
function getModeCode(mode) {
  const modeMap = {
    'drive': 0,   // 驾车
    'bus': 1,     // 公交
    'walk': 2     // 步行
  };
  return modeMap[mode] || 0;
}

// 使用示例
launchAmapNavigation(39.996893, 116.481488, '望京旗舰店', 'walk');
```

---

### 2.2 阿里云短信服务深度集成

#### 2.2.1 完整流程图

```
短信验证码完整流程
┌───────┐                 ┌──────────┐                ┌───────────┐
│ 用户  │                 │ 后端服务  │                │ 阿里云SMS │
└───┬───┘                 └─────┬────┘                └─────┬─────┘
    │                           │                           │
    │ 1. 点击"发送验证码"         │                           │
    ├──────────────────────────>│                           │
    │                           │                           │
    │                           │ 2. 检查限流（Redis）       │
    │                           │<──────────────────────────┤
    │                           │                           │
    │                           │ 3. 生成6位随机验证码       │
    │                           │<────────────┐            │
    │                           │             │            │
    │                           │ 4. 存入Redis（5分钟过期） │
    │                           │<────────────┘            │
    │                           │                           │
    │                           │ 5. 调用阿里云SMS API       │
    │                           ├──────────────────────────>│
    │                           │                           │
    │                           │                           │ 6. 发送短信
    │<──────────────────────────┼───────────────────────────┤
    │ 7. 收到短信（验证码：123456）│                           │
    │                           │                           │
    │ 8. 输入验证码并提交         │                           │
    ├──────────────────────────>│                           │
    │                           │                           │
    │                           │ 9. 从Redis读取验证码比对   │
    │                           │<──────────────────────────┤
    │                           │                           │
    │                           │ 10. 验证成功，删除验证码   │
    │                           │<────────────┐            │
    │                           │             │            │
    │ 11. 返回登录成功，发放Token │             │            │
    │<──────────────────────────┤             │            │
    │                           │                           │
```

#### 2.2.2 Redis存储结构设计

```
Redis Key设计
├── sms:code:{phone}              # 验证码存储
│   Value: "123456"
│   TTL: 300秒（5分钟）
│   用途: 验证用户输入的验证码
│
├── sms:rate_limit:{phone}        # 发送限流
│   Value: "1"
│   TTL: 60秒（1分钟）
│   用途: 防止同一手机号频繁发送
│
├── sms:daily_count:{phone}:{date} # 日发送次数
│   Value: "3"
│   TTL: 86400秒（24小时）
│   用途: 限制每个手机号每天最多发送10次
│
└── sms:ip_limit:{ip}:{date}      # IP限流
    Value: "50"
    TTL: 86400秒（24小时）
    用途: 防止恶意IP刷短信
```

#### 2.2.3 完整Service实现

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final AliyunSmsProperties smsProperties;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${sms.mock-enabled:false}")
    private boolean mockEnabled;  // Mock开关（开发环境使用）

    @Value("${sms.mock-code:123456}")
    private String mockCode;

    // 每日发送次数限制
    private static final int MAX_DAILY_SEND_COUNT = 10;

    // 每个IP每日发送次数限制
    private static final int MAX_IP_DAILY_COUNT = 50;

    /**
     * 发送验证码
     * @param phone 手机号
     * @param scene 场景: login/register/reset_password/change_phone
     * @param clientIp 客户端IP
     * @return 验证码（仅Mock模式返回）
     */
    public String sendVerificationCode(String phone, String scene, String clientIp) {
        // ========== 第一阶段：参数验证 ==========

        // 1. 验证手机号格式
        if (!isValidPhone(phone)) {
            log.warn("【短信发送】手机号格式不正确: phone={}", phone);
            throw new BusinessException("手机号格式不正确");
        }

        // 2. 验证场景参数
        List<String> allowedScenes = Arrays.asList("login", "register", "reset_password", "change_phone", "cancel_account");
        if (!allowedScenes.contains(scene)) {
            throw new BusinessException("非法的短信场景");
        }

        // ========== 第二阶段：限流检查 ==========

        // 3. 限流检查1：同一手机号1分钟内只能发送1次
        String rateLimitKey = "sms:rate_limit:" + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateLimitKey))) {
            log.warn("【短信发送】发送过于频繁: phone={}", phone);
            throw new BusinessException("发送过于频繁，请1分钟后再试");
        }

        // 4. 限流检查2：同一手机号每天最多发送10次
        String todayDateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String dailyCountKey = "sms:daily_count:" + phone + ":" + todayDateStr;
        String dailyCountStr = redisTemplate.opsForValue().get(dailyCountKey);
        int dailyCount = dailyCountStr == null ? 0 : Integer.parseInt(dailyCountStr);

        if (dailyCount >= MAX_DAILY_SEND_COUNT) {
            log.warn("【短信发送】每日发送次数超限: phone={}, count={}", phone, dailyCount);
            throw new BusinessException("今日发送次数已达上限，请明天再试");
        }

        // 5. 限流检查3：同一IP每天最多发送50次（防止恶意刷短信）
        String ipLimitKey = "sms:ip_limit:" + clientIp + ":" + todayDateStr;
        String ipCountStr = redisTemplate.opsForValue().get(ipLimitKey);
        int ipCount = ipCountStr == null ? 0 : Integer.parseInt(ipCountStr);

        if (ipCount >= MAX_IP_DAILY_COUNT) {
            log.warn("【短信发送】IP发送次数超限: ip={}, count={}", clientIp, ipCount);
            throw new BusinessException("您的操作过于频繁，请稍后再试");
        }

        // ========== 第三阶段：生成验证码 ==========

        // 6. 生成6位随机验证码
        String code = generateCode();
        log.info("【短信发送】生成验证码: phone={}, scene={}, code={}", phone, scene, code);

        // ========== 第四阶段：存储验证码 ==========

        // 7. 存入Redis（5分钟有效期）
        String codeKey = "sms:code:" + phone + ":" + scene;
        redisTemplate.opsForValue().set(codeKey, code, 5, TimeUnit.MINUTES);

        // ========== 第五阶段：发送短信 ==========

        // 8. Mock模式（开发环境）
        if (mockEnabled) {
            log.info("【短信发送】Mock模式: phone={}, code={}", phone, mockCode);
            redisTemplate.opsForValue().set(codeKey, mockCode, 5, TimeUnit.MINUTES);

            // 设置限流（避免绕过限流）
            setRateLimits(phone, clientIp, todayDateStr, dailyCount, ipCount);

            return mockCode;  // 仅Mock模式返回验证码
        }

        // 9. 调用阿里云SDK发送短信
        boolean success = sendSmsToAliyun(phone, code, scene);

        if (!success) {
            log.error("【短信发送】阿里云SMS发送失败: phone={}", phone);
            // 删除Redis中的验证码（因为短信未发送成功）
            redisTemplate.delete(codeKey);
            throw new BusinessException("短信发送失败，请稍后重试");
        }

        // ========== 第六阶段：设置限流 ==========

        // 10. 设置限流标记
        setRateLimits(phone, clientIp, todayDateStr, dailyCount, ipCount);

        log.info("【短信发送】成功: phone={}, scene={}", phone, scene);
        return null;  // 生产环境不返回验证码
    }

    /**
     * 验证验证码
     * @param phone 手机号
     * @param code 验证码
     * @param scene 场景
     * @return 是否验证成功
     */
    public boolean verifyCode(String phone, String code, String scene) {
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(code)) {
            return false;
        }

        // 1. 从Redis读取验证码
        String codeKey = "sms:code:" + phone + ":" + scene;
        String storedCode = redisTemplate.opsForValue().get(codeKey);

        // 2. 比对验证码
        if (code.equals(storedCode)) {
            // 验证成功后删除验证码（防止重复使用）
            redisTemplate.delete(codeKey);
            log.info("【短信验证】成功: phone={}, scene={}", phone, scene);
            return true;
        }

        log.warn("【短信验证】失败: phone={}, scene={}, inputCode={}, storedCode={}",
                phone, scene, code, storedCode);
        return false;
    }

    /**
     * 调用阿里云SDK发送短信
     */
    private boolean sendSmsToAliyun(String phone, String code, String scene) {
        try {
            // 1. 创建阿里云客户端
            Config config = new Config()
                    .setAccessKeyId(smsProperties.getAccessKeyId())
                    .setAccessKeySecret(smsProperties.getAccessKeySecret())
                    .setRegionId(smsProperties.getRegionId());

            Client client = new Client(config);

            // 2. 根据场景选择模板
            String templateCode = getTemplateCodeByScene(scene);

            // 3. 构建请求
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(smsProperties.getSignName())
                    .setTemplateCode(templateCode)
                    .setTemplateParam("{\"code\":\"" + code + "\"}");

            // 4. 发送请求
            SendSmsResponse response = client.sendSms(request);

            // 5. 检查响应
            boolean success = "OK".equals(response.getBody().getCode());

            if (success) {
                log.info("【阿里云SMS】发送成功: phone={}, requestId={}",
                        phone, response.getBody().getRequestId());
            } else {
                log.error("【阿里云SMS】发送失败: phone={}, code={}, message={}",
                        phone, response.getBody().getCode(), response.getBody().getMessage());
            }

            return success;

        } catch (Exception e) {
            log.error("【阿里云SMS】异常: phone={}", phone, e);
            return false;
        }
    }

    /**
     * 设置限流标记
     */
    private void setRateLimits(String phone, String clientIp, String todayDateStr,
                               int currentDailyCount, int currentIpCount) {
        // 1. 设置1分钟限流
        String rateLimitKey = "sms:rate_limit:" + phone;
        redisTemplate.opsForValue().set(rateLimitKey, "1", 1, TimeUnit.MINUTES);

        // 2. 增加每日发送次数
        String dailyCountKey = "sms:daily_count:" + phone + ":" + todayDateStr;
        redisTemplate.opsForValue().set(
            dailyCountKey,
            String.valueOf(currentDailyCount + 1),
            24,
            TimeUnit.HOURS
        );

        // 3. 增加IP每日发送次数
        String ipLimitKey = "sms:ip_limit:" + clientIp + ":" + todayDateStr;
        redisTemplate.opsForValue().set(
            ipLimitKey,
            String.valueOf(currentIpCount + 1),
            24,
            TimeUnit.HOURS
        );
    }

    /**
     * 生成6位随机验证码
     */
    private String generateCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    /**
     * 验证手机号格式
     */
    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    /**
     * 根据场景获取短信模板CODE
     */
    private String getTemplateCodeByScene(String scene) {
        Map<String, String> sceneTemplateMap = Map.of(
            "login", "SMS_123456789",           // 登录验证模板
            "register", "SMS_123456790",        // 注册验证模板
            "reset_password", "SMS_123456791",  // 重置密码模板
            "change_phone", "SMS_123456792",    // 修改手机号模板
            "cancel_account", "SMS_123456793"   // 注销账号模板
        );

        return sceneTemplateMap.getOrDefault(scene, smsProperties.getTemplateCode());
    }
}
```

**Controller使用**：
```java
@RestController
@RequestMapping("/app/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SmsService smsService;

    @Operation(summary = "发送验证码", description = "支持登录/注册/找回密码等场景")
    @PostMapping("/send-code")
    public Result<Void> sendCode(
            @RequestParam String phone,
            @RequestParam(defaultValue = "login") String scene,
            HttpServletRequest request) {

        // 获取客户端IP
        String clientIp = getClientIP(request);

        // 发送验证码
        smsService.sendVerificationCode(phone, scene, clientIp);

        return Result.success();
    }

    @Operation(summary = "用户登录", description = "手机号+验证码登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        // 1. 验证验证码
        boolean valid = smsService.verifyCode(
            loginDTO.getPhone(),
            loginDTO.getCode(),
            "login"
        );

        if (!valid) {
            return Result.fail("验证码错误或已过期");
        }

        // 2. 查询或创建用户
        User user = authService.loginOrRegister(loginDTO.getPhone());

        // 3. 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getPhone());

        // 4. 返回登录信息
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserId(user.getId());
        loginVO.setNickname(user.getNickname());
        loginVO.setAvatar(user.getAvatar());

        return Result.success(loginVO);
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
```

---

**文档版本**: v1.1
**创建日期**: 2025-01-24
**作者**: 架构团队

如有疑问，请参考主文档《技术架构与外部服务集成文档.md》或联系架构负责人。
