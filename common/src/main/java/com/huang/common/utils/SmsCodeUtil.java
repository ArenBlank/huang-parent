package com.huang.common.utils;

import com.huang.common.config.DevelopmentConfig;
import com.huang.common.constant.RedisConstant;
import com.huang.common.redis.RedisCacheSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class SmsCodeUtil {

    private static final String DEFAULT_DEV_SMS_CODE = "123456";

    private final DevelopmentConfig developmentConfig;
    private final RedisCacheSupport redisCacheSupport;

    public SmsCodeUtil(DevelopmentConfig developmentConfig, RedisCacheSupport redisCacheSupport) {
        this.developmentConfig = developmentConfig;
        this.redisCacheSupport = redisCacheSupport;
    }

    public String sendSmsCode(String phone, String type) {
        if (developmentConfig.isEnabled()) {
            String fixedCode = resolveFixedCode();
            log.info("开发模式短信验证码: phone={}, type={}, code={}", phone, type, fixedCode);
            return fixedCode;
        }

        String code = generateRandomCode();
        cacheSmsCode(phone, type, code);
        // TODO: 接入真实短信服务发送验证码
        log.info("send sms code requested, phone={}, type={}", phone, type);
        return null;
    }

    public boolean verifySmsCode(String phone, String code, String type) {
        if (developmentConfig.isEnabled() && developmentConfig.isSkipSmsValidation()) {
            boolean isValid = resolveFixedCode().equals(code);
            log.info("开发模式验证码校验: phone={}, type={}, success={}", phone, type, isValid);
            return isValid;
        }

        if (!StringUtils.hasText(phone) || !StringUtils.hasText(code) || !StringUtils.hasText(type)) {
            return false;
        }

        String codeKey = RedisConstant.appSmsCodeKey(type, phone);
        String cachedCode = redisCacheSupport.getString(codeKey);
        boolean isValid = StringUtils.hasText(cachedCode) && cachedCode.equals(code);
        if (isValid) {
            redisCacheSupport.safeDelete(codeKey);
            redisCacheSupport.safeDelete(RedisConstant.appSmsFailKey(type, phone));
            return true;
        }

        increaseFailCount(phone, type);
        log.info("verify sms code failed, phone={}, type={}", phone, type);
        return false;
    }

    private String generateRandomCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
    }

    public boolean canSendSms(String phone, String type) {
        if (developmentConfig.isEnabled()) {
            return true;
        }
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(type)) {
            return false;
        }
        return !StringUtils.hasText(redisCacheSupport.getString(RedisConstant.appSmsCooldownKey(type, phone)));
    }

    private void cacheSmsCode(String phone, String type, String code) {
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(type) || !StringUtils.hasText(code)) {
            return;
        }
        redisCacheSupport.setString(
                RedisConstant.appSmsCodeKey(type, phone),
                code,
                RedisConstant.APP_LOGIN_CODE_TTL_SEC
        );
        redisCacheSupport.setString(
                RedisConstant.appSmsCooldownKey(type, phone),
                "1",
                RedisConstant.APP_LOGIN_CODE_RESEND_TIME_SEC
        );
        redisCacheSupport.safeDelete(RedisConstant.appSmsFailKey(type, phone));
    }

    private void increaseFailCount(String phone, String type) {
        String failKey = RedisConstant.appSmsFailKey(type, phone);
        String current = redisCacheSupport.getString(failKey);
        int failCount = 0;
        if (StringUtils.hasText(current)) {
            try {
                failCount = Integer.parseInt(current);
            } catch (NumberFormatException ignore) {
                failCount = 0;
            }
        }
        redisCacheSupport.setString(failKey, String.valueOf(failCount + 1), RedisConstant.APP_LOGIN_CODE_TTL_SEC);
    }

    private String resolveFixedCode() {
        String fixedCode = developmentConfig.getFixedSmsCode();
        return StringUtils.hasText(fixedCode) ? fixedCode : DEFAULT_DEV_SMS_CODE;
    }
}
