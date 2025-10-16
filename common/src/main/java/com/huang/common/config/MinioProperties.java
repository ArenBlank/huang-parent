package com.huang.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO配置属性类
 * @author system
 * @since 2025-01-24
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * MinIO服务地址
     */
    private String endpoint;

    /**
     * 访问密钥
     */
    private String accessKey;

    /**
     * 密钥
     */
    private String secretKey;

    /**
     * 存储桶名称
     */
    private String bucketName;
}
