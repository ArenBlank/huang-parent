package com.huang.web.app.service.biz.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huang.model.entity.User;
import com.huang.web.app.service.core.UserCoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Service
public class AppAuthCacheService {

    private static final Logger log = LoggerFactory.getLogger(AppAuthCacheService.class);
    private static final String APP_AUTH_USER_KEY_PREFIX = "app:auth:user:";
    private static final long TTL_SEC = 7200L;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final UserCoreService userCoreService;

    public AppAuthCacheService(StringRedisTemplate stringRedisTemplate,
                               ObjectMapper objectMapper,
                               UserCoreService userCoreService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.userCoreService = userCoreService;
    }

    public AppAuthSnapshot getOrLoad(Long userId) {
        if (userId == null) {
            return null;
        }
        String key = authKey(userId);
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(json)) {
            AppAuthSnapshot cached = readSnapshot(json, userId);
            if (cached != null) {
                return cached;
            }
            stringRedisTemplate.delete(key);
        }

        User user = userCoreService.getById(userId);
        if (user == null) {
            return null;
        }
        AppAuthSnapshot snapshot = new AppAuthSnapshot(user.getStatus(), normalizeTokenVersion(user.getTokenVersion()));
        stringRedisTemplate.opsForValue().set(key, writeSnapshot(snapshot), Duration.ofSeconds(TTL_SEC));
        return snapshot;
    }

    public void evict(Long userId) {
        if (userId == null) {
            return;
        }
        stringRedisTemplate.delete(authKey(userId));
    }

    public static String authKey(Long userId) {
        return APP_AUTH_USER_KEY_PREFIX + userId;
    }

    private AppAuthSnapshot readSnapshot(String json, Long userId) {
        try {
            return objectMapper.readValue(json, AppAuthSnapshot.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to read cached app auth snapshot for user {}, cache will be rebuilt", userId, e);
            return null;
        }
    }

    private String writeSnapshot(AppAuthSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize app auth snapshot", e);
        }
    }

    private int normalizeTokenVersion(Integer tokenVersion) {
        return tokenVersion == null || tokenVersion < 0 ? 0 : tokenVersion;
    }
}
