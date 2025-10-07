package com.huang.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token黑名单服务
 * 用于管理已退出登录的token，防止已退出的token继续使用
 * 
 * @author system
 * @since 2025-01-24
 */
@Slf4j
@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 将token加入黑名单
     * @param token JWT token
     * @param ttlSeconds token剩余有效期（秒）
     */
    public void addToBlacklist(String token, long ttlSeconds) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("尝试将空token加入黑名单");
            return;
        }
        
        if (ttlSeconds <= 0) {
            log.warn("token TTL无效，不加入黑名单: {}", ttlSeconds);
            return;
        }
        
        String key = BLACKLIST_PREFIX + token;
        String tokenPreview = token.length() > 16 ? token.substring(0, 8) + "..." + token.substring(token.length()-8) : token;
        
        try {
            stringRedisTemplate.opsForValue().set(key, "1", ttlSeconds, TimeUnit.SECONDS);
            log.info("token已加入黑名单: {}, TTL: {}秒, Redis Key: {}", 
                    tokenPreview, ttlSeconds, key.substring(0, Math.min(50, key.length())) + "...");
        } catch (Exception e) {
            log.error("将token加入黑名单失败: {}", tokenPreview, e);
        }
    }

    /**
     * 检查token是否在黑名单中
     * @param token JWT token
     * @return true-在黑名单中，false-不在黑名单中
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        String key = BLACKLIST_PREFIX + token;
        String tokenPreview = token.length() > 16 ? token.substring(0, 8) + "..." + token.substring(token.length()-8) : token;
        
        try {
            Boolean exists = stringRedisTemplate.hasKey(key);
            boolean result = Boolean.TRUE.equals(exists);
            
            log.info("检查token黑名单状态: {} -> {}, Redis Key: {}", 
                    tokenPreview, result ? "在黑名单中" : "不在黑名单中", 
                    key.substring(0, Math.min(50, key.length())) + "...");
            
            return result;
        } catch (Exception e) {
            log.error("检查token黑名单状态失败: {}", tokenPreview, e);
            // Redis异常时，为了安全起见，假定token有效
            return false;
        }
    }

    /**
     * 从黑名单中移除token（一般不需要，让其自然过期即可）
     * @param token JWT token
     */
    public void removeFromBlacklist(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        
        String key = BLACKLIST_PREFIX + token;
        try {
            stringRedisTemplate.delete(key);
            log.info("token已从黑名单中移除");
        } catch (Exception e) {
            log.error("从黑名单中移除token失败", e);
        }
    }

    /**
     * 清理指定用户的所有黑名单token（可选功能）
     * 注意：此方法需要扫描所有key，性能开销大，谨慎使用
     * @param userId 用户ID
     */
    public void clearUserBlacklist(Long userId) {
        // 由于Redis key中不包含userId，此方法实现复杂
        // 如需此功能，建议在key设计时加入userId信息
        log.warn("clearUserBlacklist功能暂未实现，建议重新设计key结构");
    }
}