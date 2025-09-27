package com.huang.common.utils;

import com.huang.common.config.DevelopmentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 短信验证码工具类
 * @author system
 * @since 2025-01-24
 */
@Slf4j
@Component
public class SmsCodeUtil {

    @Autowired
    private DevelopmentConfig developmentConfig;

    /**
     * 发送短信验证码
     * @param phone 手机号
     * @param type 验证码类型
     * @return 验证码（仅开发模式返回）
     */
    public String sendSmsCode(String phone, String type) {
        if (developmentConfig.isEnabled()) {
            log.info("【开发模式】向手机号 {} 发送 {} 类型验证码: {}", phone, type, developmentConfig.getFixedSmsCode());
            return developmentConfig.getFixedSmsCode();
        }
        
        // 生产环境下发送真实短信
        String code = generateRandomCode();
        // TODO: 调用第三方短信服务发送验证码
        log.info("向手机号 {} 发送 {} 类型验证码", phone, type);
        
        return null; // 生产环境不返回验证码
    }

    /**
     * 验证短信验证码
     * @param phone 手机号
     * @param code 验证码
     * @param type 验证码类型
     * @return 是否验证通过
     */
    public boolean verifySmsCode(String phone, String code, String type) {
        if (developmentConfig.isEnabled() && developmentConfig.isSkipSmsValidation()) {
            boolean isValid = developmentConfig.getFixedSmsCode().equals(code);
            log.info("【开发模式】验证手机号 {} 的 {} 类型验证码 {}: {}", phone, type, code, isValid ? "通过" : "失败");
            return isValid;
        }
        
        // 生产环境下从Redis或数据库验证
        // TODO: 实现真实的验证码验证逻辑
        log.info("验证手机号 {} 的 {} 类型验证码", phone, type);
        
        return false; // 临时返回，实际需要实现验证逻辑
    }

    /**
     * 生成6位随机验证码
     * @return 验证码
     */
    private String generateRandomCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));
    }

    /**
     * 检查验证码发送频率限制
     * @param phone 手机号
     * @param type 验证码类型
     * @return 是否可以发送
     */
    public boolean canSendSms(String phone, String type) {
        if (developmentConfig.isEnabled()) {
            // 开发模式下不限制发送频率
            return true;
        }
        
        // 生产环境下检查发送频率限制
        // TODO: 实现频率限制检查逻辑
        return true;
    }
}