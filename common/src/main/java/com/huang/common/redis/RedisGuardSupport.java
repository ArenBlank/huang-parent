package com.huang.common.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class RedisGuardSupport {

    public static final String NOOP_LOCK_TOKEN = "noop";

    public enum GuardDecision {
        ALLOW,
        BLOCK,
        DEGRADED
    }

    private final StringRedisTemplate stringRedisTemplate;

    public RedisGuardSupport(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean allowByFixedWindow(String key, long maxRequests, long windowSec) {
        GuardDecision decision = fixedWindowDecision(key, maxRequests, windowSec);
        return decision != GuardDecision.BLOCK;
    }

    public GuardDecision fixedWindowDecision(String key, long maxRequests, long windowSec) {
        if (key == null || key.isBlank() || maxRequests <= 0 || windowSec <= 0) {
            return GuardDecision.ALLOW;
        }
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count == null) {
                return GuardDecision.DEGRADED;
            }
            if (count == 1L) {
                stringRedisTemplate.expire(key, Duration.ofSeconds(windowSec));
            }
            return count <= maxRequests ? GuardDecision.ALLOW : GuardDecision.BLOCK;
        } catch (Exception e) {
            log.warn("redis fixed-window guard failed, key={}", key, e);
            return GuardDecision.DEGRADED;
        }
    }

    public boolean tryAcquireIdempotent(String key, long ttlSec) {
        GuardDecision decision = idempotentDecision(key, ttlSec);
        return decision != GuardDecision.BLOCK && decision != GuardDecision.DEGRADED;
    }

    public GuardDecision idempotentDecision(String key, long ttlSec) {
        if (key == null || key.isBlank() || ttlSec <= 0) {
            return GuardDecision.ALLOW;
        }
        try {
            Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(ttlSec));
            return Boolean.TRUE.equals(ok) ? GuardDecision.ALLOW : GuardDecision.BLOCK;
        } catch (Exception e) {
            log.warn("redis idempotent guard failed, key={}", key, e);
            return GuardDecision.DEGRADED;
        }
    }

    public String tryAcquireLock(String key, long ttlSec) {
        if (key == null || key.isBlank() || ttlSec <= 0) {
            return NOOP_LOCK_TOKEN;
        }
        String token = UUID.randomUUID().toString();
        try {
            Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, token, Duration.ofSeconds(ttlSec));
            return Boolean.TRUE.equals(ok) ? token : null;
        } catch (Exception e) {
            log.warn("redis lock guard failed, key={}", key, e);
            return NOOP_LOCK_TOKEN;
        }
    }

    public void releaseLock(String key, String token) {
        if (key == null || key.isBlank() || token == null || token.isBlank() || NOOP_LOCK_TOKEN.equals(token)) {
            return;
        }
        try {
            String current = stringRedisTemplate.opsForValue().get(key);
            if (Objects.equals(current, token)) {
                stringRedisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.warn("redis lock release failed, key={}", key, e);
        }
    }
}
