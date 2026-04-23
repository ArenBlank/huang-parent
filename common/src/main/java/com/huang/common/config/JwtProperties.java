package com.huang.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置，统一通过环境变量/配置文件注入。
 */
@ConfigurationProperties(prefix = "fitness.jwt")
public class JwtProperties {

    private String secret;
    private long appAccessExpireMs = 2 * 60 * 60 * 1000L;
    private long appRefreshExpireMs = 15L * 24 * 60 * 60 * 1000L;
    private long adminAccessExpireMs = 30 * 60 * 1000L;
    private long adminRefreshExpireMs = 7L * 24 * 60 * 60 * 1000L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAppAccessExpireMs() {
        return appAccessExpireMs;
    }

    public void setAppAccessExpireMs(long appAccessExpireMs) {
        this.appAccessExpireMs = appAccessExpireMs;
    }

    public long getAppRefreshExpireMs() {
        return appRefreshExpireMs;
    }

    public void setAppRefreshExpireMs(long appRefreshExpireMs) {
        this.appRefreshExpireMs = appRefreshExpireMs;
    }

    public long getAdminAccessExpireMs() {
        return adminAccessExpireMs;
    }

    public void setAdminAccessExpireMs(long adminAccessExpireMs) {
        this.adminAccessExpireMs = adminAccessExpireMs;
    }

    public long getAdminRefreshExpireMs() {
        return adminRefreshExpireMs;
    }

    public void setAdminRefreshExpireMs(long adminRefreshExpireMs) {
        this.adminRefreshExpireMs = adminRefreshExpireMs;
    }
}
