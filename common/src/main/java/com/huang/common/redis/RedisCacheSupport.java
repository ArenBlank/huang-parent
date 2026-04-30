package com.huang.common.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huang.common.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class RedisCacheSupport {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheSupport(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> CacheValue<T> getJson(String key, Class<T> clazz) {
        return getJson(key, objectMapper.getTypeFactory().constructType(clazz));
    }

    public <T> CacheValue<T> getJson(String key, TypeReference<T> typeReference) {
        return getJson(key, objectMapper.getTypeFactory().constructType(typeReference));
    }

    private <T> CacheValue<T> getJson(String key, com.fasterxml.jackson.databind.JavaType javaType) {
        String raw = getRawValue(key);
        if (raw == null) {
            return CacheValue.miss();
        }
        if (RedisConstant.CACHE_NULL_VALUE.equals(raw)) {
            return CacheValue.cachedNull();
        }
        try {
            return CacheValue.hit(objectMapper.readValue(raw, javaType));
        } catch (Exception e) {
            log.warn("redis json decode failed, key={}", key, e);
            safeDelete(key);
            return CacheValue.miss();
        }
    }

    public String getString(String key) {
        return getRawValue(key);
    }

    public List<String> multiGet(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return List.of();
        }
        try {
            List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
            return values == null ? Collections.emptyList() : values;
        } catch (Exception e) {
            log.warn("redis multiGet failed, keys={}", keys, e);
            return Collections.emptyList();
        }
    }

    public void setJson(String key, Object value, long ttlSec) {
        if (value == null) {
            cacheNull(key, ttlSec);
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), Duration.ofSeconds(ttlSec));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize redis cache value failed", e);
        } catch (Exception e) {
            log.warn("redis set json failed, key={}", key, e);
        }
    }

    public void setString(String key, String value, long ttlSec) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSec));
        } catch (Exception e) {
            log.warn("redis set string failed, key={}", key, e);
        }
    }

    public void cacheNull(String key, long ttlSec) {
        setString(key, RedisConstant.CACHE_NULL_VALUE, ttlSec);
    }

    public void safeDelete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("redis delete failed, key={}", key, e);
        }
    }

    public void safeDelete(Collection<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return;
        }
        try {
            stringRedisTemplate.delete(keys);
        } catch (Exception e) {
            log.warn("redis batch delete failed, keys={}", keys, e);
        }
    }

    public void deleteByPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return;
        }
        try {
            Set<String> keys = scanKeysByPrefix(prefix);
            if (!CollectionUtils.isEmpty(keys)) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("redis delete by prefix failed, prefix={}", prefix, e);
        }
    }

    public boolean tryLock(String key, String token, long ttlSec) {
        try {
            Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, token, Duration.ofSeconds(ttlSec));
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.warn("redis tryLock failed, key={}", key, e);
            return false;
        }
    }

    public String newLockToken() {
        return UUID.randomUUID().toString();
    }

    public void unlock(String key, String token) {
        if (key == null || token == null) {
            return;
        }
        try {
            compareAndDelete(key, token);
        } catch (Exception e) {
            log.warn("redis unlock failed, key={}", key, e);
        }
    }

    public long ttlWithJitter(long baseSec, long maxJitterSec) {
        if (maxJitterSec <= 0) {
            return baseSec;
        }
        return baseSec + ThreadLocalRandom.current().nextLong(maxJitterSec + 1);
    }

    private String getRawValue(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("redis get failed, key={}", key, e);
            return null;
        }
    }

    private Set<String> scanKeysByPrefix(String prefix) {
        return stringRedisTemplate.execute((RedisCallback<Set<String>>) connection -> doScan(connection, prefix));
    }

    private boolean compareAndDelete(String key, String token) {
        byte[] rawKey = stringRedisTemplate.getStringSerializer().serialize(key);
        byte[] rawToken = stringRedisTemplate.getStringSerializer().serialize(token);
        if (rawKey == null || rawToken == null) {
            return false;
        }
        Boolean deleted = stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {
            connection.watch(rawKey);
            byte[] current = connection.get(rawKey);
            if (!Arrays.equals(current, rawToken)) {
                connection.unwatch();
                return false;
            }
            connection.multi();
            connection.del(rawKey);
            List<Object> exec = connection.exec();
            return exec != null && !exec.isEmpty();
        });
        return Boolean.TRUE.equals(deleted);
    }

    private Set<String> doScan(RedisConnection connection, String prefix) {
        Set<String> keys = new LinkedHashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(prefix + "*")
                .count(200)
                .build();
        try (Cursor<byte[]> cursor = connection.scan(options)) {
            while (cursor.hasNext()) {
                byte[] rawKey = cursor.next();
                String key = stringRedisTemplate.getStringSerializer().deserialize(rawKey);
                if (key != null) {
                    keys.add(key);
                }
            }
        } catch (Exception e) {
            log.warn("redis scan failed, prefix={}", prefix, e);
        }
        return keys;
    }

    public record CacheValue<T>(boolean found, boolean nullValue, T value) {
        public static <T> CacheValue<T> hit(T value) {
            return new CacheValue<>(true, false, value);
        }

        public static <T> CacheValue<T> cachedNull() {
            return new CacheValue<>(true, true, null);
        }

        public static <T> CacheValue<T> miss() {
            return new CacheValue<>(false, false, null);
        }
    }
}
