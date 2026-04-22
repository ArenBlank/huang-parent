package com.huang.web.admin.controller;

import com.huang.common.minio.MinioProperties;
import com.huang.common.result.Result;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminPermission;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.custom.aop.OperationLog;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Admin Upload", description = "Admin image upload")
@Slf4j
@Validated
@RestController
@RequestMapping("/admin/upload")
@RequireAdminRole({AdminRoleCode.ADMIN, AdminRoleCode.OPS_ADMIN})
public class AdminUploadController {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public AdminUploadController(ObjectProvider<MinioClient> minioClientProvider,
                                 MinioProperties minioProperties) {
        this.minioClient = minioClientProvider.getIfAvailable();
        this.minioProperties = minioProperties;
    }

    @Operation(summary = "Upload image to MinIO")
    @PostMapping("/image")
    @RequireAdminPermission({"course:create", "course:update"})
    @OperationLog(module = "upload", action = "image_upload", detail = "admin upload image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail("请选择图片文件");
        }
        if (!isImageFile(file)) {
            return Result.fail("仅支持 jpg、jpeg、png、webp、gif 图片");
        }
        if (minioClient == null || !StringUtils.hasText(minioProperties.getBucketName())) {
            return Result.fail("文件存储未配置");
        }

        String objectPath = buildImageObjectPath(file.getOriginalFilename());
        try {
            ensureBucketExists();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectPath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(resolveContentType(file))
                            .build()
            );
            return Result.ok(buildPublicUrl(objectPath));
        } catch (Exception e) {
            log.error("admin upload image failed, objectPath={}", objectPath, e);
            return Result.fail("图片上传失败，请稍后重试");
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build()
        );
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build()
            );
        }
        ensureBucketPublicReadable();
    }

    private void ensureBucketPublicReadable() throws Exception {
        String bucketName = minioProperties.getBucketName();
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": "*",
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucketName);
        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(policy)
                        .build()
        );
    }

    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && contentType.toLowerCase().startsWith("image/")) {
            return true;
        }
        String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        return ".jpg".equals(extension)
                || ".jpeg".equals(extension)
                || ".png".equals(extension)
                || ".webp".equals(extension)
                || ".gif".equals(extension);
    }

    private String buildImageObjectPath(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String datePath = LocalDate.now().toString().replace("-", "");
        return "images/upload/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
    }

    private String getFileExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return ".png";
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    private String resolveContentType(MultipartFile file) {
        return StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream";
    }

    private String buildPublicUrl(String objectPath) {
        String base = StringUtils.hasText(minioProperties.getPublicEndpoint())
                ? minioProperties.getPublicEndpoint()
                : minioProperties.getEndpoint();
        String safeBase = trimTrailingSlash(base);
        String safeObjectPath = objectPath.startsWith("/") ? objectPath.substring(1) : objectPath;
        return safeBase + "/" + minioProperties.getBucketName() + "/" + safeObjectPath;
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
