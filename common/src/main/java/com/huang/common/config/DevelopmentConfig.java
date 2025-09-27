package com.huang.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 开发配置类
 * @author system
 * @since 2025-01-24
 */
@Component
@ConfigurationProperties(prefix = "development")
public class DevelopmentConfig {

    /**
     * 是否为开发模式
     */
    private boolean enabled = true;

    /**
     * 开发模式下的固定验证码
     */
    private String fixedSmsCode = "123456";

    /**
     * 是否跳过短信验证码验证
     */
    private boolean skipSmsValidation = true;

    /**
     * 是否跳过图形验证码验证
     */
    private boolean skipCaptchaValidation = true;

    /**
     * 开发模式下的测试用户名
     */
    private String testUsername = "testuser";

    /**
     * 开发模式下的测试密码
     */
    private String testPassword = "123456";

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFixedSmsCode() {
        return fixedSmsCode;
    }

    public void setFixedSmsCode(String fixedSmsCode) {
        this.fixedSmsCode = fixedSmsCode;
    }

    public boolean isSkipSmsValidation() {
        return skipSmsValidation;
    }

    public void setSkipSmsValidation(boolean skipSmsValidation) {
        this.skipSmsValidation = skipSmsValidation;
    }

    public boolean isSkipCaptchaValidation() {
        return skipCaptchaValidation;
    }

    public void setSkipCaptchaValidation(boolean skipCaptchaValidation) {
        this.skipCaptchaValidation = skipCaptchaValidation;
    }

    public String getTestUsername() {
        return testUsername;
    }

    public void setTestUsername(String testUsername) {
        this.testUsername = testUsername;
    }

    public String getTestPassword() {
        return testPassword;
    }

    public void setTestPassword(String testPassword) {
        this.testPassword = testPassword;
    }
}