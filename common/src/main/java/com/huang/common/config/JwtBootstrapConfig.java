package com.huang.common.config;

import com.huang.common.utils.JwtUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 应用启动时将 JWT 配置写入 JwtUtil，保留静态调用方式并支持环境化配置。
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtBootstrapConfig {

    private final JwtProperties jwtProperties;

    public JwtBootstrapConfig(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        JwtUtil.configure(
                jwtProperties.getSecret(),
                jwtProperties.getAppAccessExpireMs(),
                jwtProperties.getAppRefreshExpireMs(),
                jwtProperties.getAdminAccessExpireMs(),
                jwtProperties.getAdminRefreshExpireMs()
        );
    }
}
