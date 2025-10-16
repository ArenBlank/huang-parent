package com.huang.common.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO客户端配置类
 * @author system
 * @since 2025-01-24
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@RequiredArgsConstructor
public class MinioConfiguration {

    private final MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient() {
        log.info("初始化MinIO客户端: endpoint={}, bucket={}",
                minioProperties.getEndpoint(),
                minioProperties.getBucketName());

        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }
}
