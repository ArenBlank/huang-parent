package com.huang.common.util;

import com.huang.common.config.MinioProperties;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO文件上传工具类
 * @author system
 * @since 2025-01-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 初始化存储桶(如果不存在则创建)
     */
    public void initBucket() {
        try {
            String bucketName = minioProperties.getBucketName();
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!exists) {
                // 创建存储桶
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );

                // 设置存储桶为公开读取
                String policy = """
                        {
                            "Version": "2012-10-17",
                            "Statement": [
                                {
                                    "Effect": "Allow",
                                    "Principal": {"AWS": "*"},
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

                log.info("成功创建MinIO存储桶: {}", bucketName);
            } else {
                log.info("MinIO存储桶已存在: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("初始化MinIO存储桶失败", e);
            throw new RuntimeException("初始化MinIO存储桶失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件
     * @param file 文件
     * @param folder 文件夹路径(如: store/images)
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            // 确保存储桶存在
            initBucket();

            // 获取原始文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new RuntimeException("文件名不能为空");
            }

            // 获取文件扩展名
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

            // 生成唯一文件名: folder/yyyyMMdd/UUID.ext
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String fileName = String.format("%s/%s/%s%s",
                    folder,
                    datePath,
                    UUID.randomUUID().toString().replace("-", ""),
                    extension
            );

            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // 构建文件访问URL
            String fileUrl = String.format("%s/%s/%s",
                    minioProperties.getEndpoint(),
                    minioProperties.getBucketName(),
                    fileName
            );

            log.info("文件上传成功: {}", fileUrl);
            return fileUrl;

        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     * @param fileName 文件名(包含路径)
     */
    public void deleteFile(String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .build()
            );
            log.info("文件删除成功: {}", fileName);
        } catch (Exception e) {
            log.error("文件删除失败", e);
            throw new RuntimeException("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件预签名URL(用于临时访问私有文件)
     * @param fileName 文件名
     * @param expires 过期时间(秒)
     * @return 预签名URL
     */
    public String getPresignedUrl(String fileName, int expires) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .expiry(expires, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("获取预签名URL失败", e);
            throw new RuntimeException("获取预签名URL失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     * @param fileName 文件名
     * @return 文件输入流
     */
    public InputStream downloadFile(String fileName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new RuntimeException("文件下载失败: " + e.getMessage());
        }
    }

    /**
     * 判断文件是否存在
     * @param fileName 文件名
     * @return 是否存在
     */
    public boolean fileExists(String fileName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
