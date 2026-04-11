package com.huang.common.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.huang.common.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LocalCacheSupport {

    private final Cache<String, LocalCacheEntry> cache;
    private final ObjectMapper objectMapper;

    public LocalCacheSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfter(new Expiry<String, LocalCacheEntry>() {
                    @Override
                    public long expireAfterCreate(String key, LocalCacheEntry value, long currentTime) {
                        return value.ttlNanos();
                    }

                    @Override
                    public long expireAfterUpdate(String key, LocalCacheEntry value, long currentTime, long currentDuration) {
                        return value.ttlNanos();
                    }

                    @Override
                    public long expireAfterRead(String key, LocalCacheEntry value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    public <T> RedisCacheSupport.CacheValue<T> getJson(String key, Class<T> clazz) {
        return getJson(key, objectMapper.getTypeFactory().constructType(clazz));
    }

    public <T> RedisCacheSupport.CacheValue<T> getJson(String key, TypeReference<T> typeReference) {
        return getJson(key, objectMapper.getTypeFactory().constructType(typeReference));
    }

    private <T> RedisCacheSupport.CacheValue<T> getJson(String key, com.fasterxml.jackson.databind.JavaType javaType) {
        String raw = getRawValue(key);
        if (raw == null) {
            return RedisCacheSupport.CacheValue.miss();
        }
        if (RedisConstant.CACHE_NULL_VALUE.equals(raw)) {
            return RedisCacheSupport.CacheValue.cachedNull();
        }
        try {
            return RedisCacheSupport.CacheValue.hit(objectMapper.readValue(raw, javaType));
        } catch (Exception e) {
            log.warn("local cache json decode failed, key={}", key, e);
            safeDelete(key);
            return RedisCacheSupport.CacheValue.miss();
        }
    }

    public String getString(String key) {
        return getRawValue(key);
    }

    public List<String> multiGet(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return List.of();
        }
        return keys.stream().map(this::getRawValue).toList();
    }

    public void setJson(String key, Object value, long ttlSec) {
        if (value == null) {
            cacheNull(key, ttlSec);
            return;
        }
        try {
            putRaw(key, objectMapper.writeValueAsString(value), ttlSec);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize local cache value failed", e);
        }
    }

    public void setString(String key, String value, long ttlSec) {
        putRaw(key, value, ttlSec);
    }

    public void cacheNull(String key, long ttlSec) {
        putRaw(key, RedisConstant.CACHE_NULL_VALUE, ttlSec);
    }

    public void safeDelete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        cache.invalidate(key);
    }

    public void safeDelete(Collection<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return;
        }
        cache.invalidateAll(keys);
    }

    public void deleteByPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return;
        }
        cache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
    }

    private void putRaw(String key, String raw, long ttlSec) {
        if (key == null || key.isBlank() || raw == null || ttlSec <= 0) {
            return;
        }
        cache.put(key, new LocalCacheEntry(raw, TimeUnit.SECONDS.toNanos(ttlSec)));
    }

    private String getRawValue(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        LocalCacheEntry entry = cache.getIfPresent(key);
        return entry == null ? null : entry.rawValue();
    }

    private record LocalCacheEntry(String rawValue, long ttlNanos) {
        private LocalCacheEntry {
            Objects.requireNonNull(rawValue, "rawValue");
        }
    }
}
