package com.huang.common.utils;

import com.huang.common.config.DevelopmentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Map;

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
    
    // 用于开发模式下的验证码缓存（生产环境应使用Redis）
    private static final Map<String, SmsCodeInfo> CODE_CACHE = new ConcurrentHashMap<>();
    
    // 发送频率缓存
    private static final Map<String, Long> SEND_RATE_CACHE = new ConcurrentHashMap<>();
    
    // 验证码有效期（5分钟）
    private static final long CODE_EXPIRE_MINUTES = 5;
    
    // 发送间隔（60秒）
    private static final long SEND_INTERVAL_SECONDS = 60;
    
    /**
     * 验证码信息存储类
     */
    private static class SmsCodeInfo {
        private String code;
        private String type;
        private LocalDateTime createTime;
        private LocalDateTime expireTime;
        
        public SmsCodeInfo(String code, String type) {
            this.code = code;
            this.type = type;
            this.createTime = LocalDateTime.now();
            this.expireTime = this.createTime.plusMinutes(CODE_EXPIRE_MINUTES);
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expireTime);
        }
        
        public boolean isValidType(String type) {
            return this.type.equals(type);
        }
        
        public boolean isValidCode(String code) {
            return this.code.equals(code);
        }
        
        // getters
        public String getCode() { return code; }
        public String getType() { return type; }
        public LocalDateTime getCreateTime() { return createTime; }
        public LocalDateTime getExpireTime() { return expireTime; }
    }

    /**
     * 发送短信验证码
     * @param phone 手机号
     * @param type 验证码类型
     * @return 验证码（仅开发模式返回）
     */
    public String sendSmsCode(String phone, String type) {
        // 检查发送频率限制
        if (!canSendSms(phone, type)) {
            log.warn("手机号 {} 发送 {} 类型验证码超过频率限制", phone, type);
            return null;
        }
        
        String code;
        
        if (developmentConfig.isEnabled()) {
            // 开发模式使用固定验证码
            code = developmentConfig.getFixedSmsCode();
            log.info("【开发模式】向手机号 {} 发送 {} 类型验证码: {}", phone, type, code);
        } else {
            // 生产环境下生成随机验证码
            code = generateRandomCode();
            
            // 调用第三方短信服务发送验证码
            boolean sendSuccess = sendSmsToThirdParty(phone, code, type);
            if (!sendSuccess) {
                log.error("向手机号 {} 发送 {} 类型验证码失败", phone, type);
                return null;
            }
            
            log.info("向手机号 {} 发送 {} 类型验证码成功", phone, type);
        }
        
        // 存储验证码到缓存
        String cacheKey = buildCacheKey(phone, type);
        CODE_CACHE.put(cacheKey, new SmsCodeInfo(code, type));
        
        // 更新发送时间记录
        SEND_RATE_CACHE.put(phone + ":" + type, System.currentTimeMillis());
        
        // 开发模式下返回验证码，生产环境不返回
        return developmentConfig.isEnabled() ? code : null;
    }

    /**
     * 验证短信验证码
     * @param phone 手机号
     * @param code 验证码
     * @param type 验证码类型
     * @return 是否验证通过
     */
    public boolean verifySmsCode(String phone, String code, String type) {
        if (phone == null || code == null || type == null) {
            log.warn("验证码验证参数不完整: phone={}, code={}, type={}", phone, code, type);
            return false;
        }
        
        // 开发模式下使用缓存验证
        String cacheKey = buildCacheKey(phone, type);
        SmsCodeInfo cacheInfo = CODE_CACHE.get(cacheKey);
        
        if (cacheInfo == null) {
            log.warn("手机号 {} 的 {} 类型验证码不存在或已过期", phone, type);
            return false;
        }
        
        if (cacheInfo.isExpired()) {
            log.warn("手机号 {} 的 {} 类型验证码已过期", phone, type);
            // 清除过期验证码
            CODE_CACHE.remove(cacheKey);
            return false;
        }
        
        if (!cacheInfo.isValidType(type)) {
            log.warn("手机号 {} 的验证码类型不匹配: 期望={}, 实际={}", phone, type, cacheInfo.getType());
            return false;
        }
        
        boolean isValid = cacheInfo.isValidCode(code);
        
        if (isValid) {
            // 验证成功后删除验证码（一次性使用）
            CODE_CACHE.remove(cacheKey);
            log.info("手机号 {} 的 {} 类型验证码验证成功", phone, type);
        } else {
            log.warn("手机号 {} 的 {} 类型验证码验证失败: 输入={}, 期望={}", 
                    phone, type, code, cacheInfo.getCode());
        }
        
        return isValid;
    }

    /**
     * 生成6位随机验证码
     * @return 验证码
     */
    private String generateRandomCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));
    }
    
    /**
     * 构建缓存key
     * @param phone 手机号
     * @param type 类型
     * @return 缓存key
     */
    private String buildCacheKey(String phone, String type) {
        return "sms:" + phone + ":" + type;
    }
    
    /**
     * 调用第三方短信服务发送验证码
     * @param phone 手机号
     * @param code 验证码
     * @param type 类型
     * @return 是否成功
     */
    private boolean sendSmsToThirdParty(String phone, String code, String type) {
        // 这里可以集成阿里云、腾讯云、七牛云等短信服务
        // 目前返回模拟成功，实际需要根据选择的服务商实现
        
        try {
            // 模拟网络请求延迟
            Thread.sleep(100);
            
            // 这里可以添加真实的短信发送逻辑
            // 例如：
            // AliyunSmsClient.sendSms(phone, code, getTemplateByType(type));
            // TencentSmsClient.sendSms(phone, code, getTemplateByType(type));
            
            log.info("模拟发送短信成功: phone={}, code={}, type={}", phone, code, type);
            return true;
            
        } catch (Exception e) {
            log.error("发送短信失败: phone={}, type={}", phone, type, e);
            return false;
        }
    }
    
    /**
     * 清理过期的缓存数据
     */
    public void cleanExpiredCache() {
        long currentTime = System.currentTimeMillis();
        
        // 清理过期的验证码
        CODE_CACHE.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        // 清理过期的发送频率记录（24小时前的）
        SEND_RATE_CACHE.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > 24 * 60 * 60 * 1000);
            
        log.debug("清理过期缓存完成，当前验证码缓存数量: {}, 频率限制缓存数量: {}", 
               CODE_CACHE.size(), SEND_RATE_CACHE.size());
    }

    /**
     * 检查验证码发送频率限制
     * @param phone 手机号
     * @param type 验证码类型
     * @return 是否可以发送
     */
    public boolean canSendSms(String phone, String type) {
        if (phone == null || type == null) {
            return false;
        }
        
        if (developmentConfig.isEnabled()) {
            // 开发模式下也要有一定的频率限制，但相对宽松
            return checkSendRate(phone, type, 10); // 10秒间隔
        }
        
        // 生产环境下检查发送频率限制
        return checkSendRate(phone, type, SEND_INTERVAL_SECONDS);
    }
    
    /**
     * 检查发送频率
     * @param phone 手机号
     * @param type 类型
     * @param intervalSeconds 间隔秒数
     * @return 是否可以发送
     */
    private boolean checkSendRate(String phone, String type, long intervalSeconds) {
        String rateKey = phone + ":" + type;
        Long lastSendTime = SEND_RATE_CACHE.get(rateKey);
        
        if (lastSendTime == null) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeDiff = (currentTime - lastSendTime) / 1000; // 转为秒
        
        boolean canSend = timeDiff >= intervalSeconds;
        if (!canSend) {
            long remainingSeconds = intervalSeconds - timeDiff;
            log.warn("手机号 {} 的 {} 类型验证码发送过于频繁，还需等待 {} 秒", phone, type, remainingSeconds);
        }
        
        return canSend;
    }
}