package com.huang.common.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.huang.common.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
public class MultiLevelCacheSupport {

    private final LocalCacheSupport localCacheSupport;
    private final RedisCacheSupport redisCacheSupport;
    private final TransactionHelper transactionHelper;
    private final CacheInvalidationPublisher cacheInvalidationPublisher;

    public MultiLevelCacheSupport(LocalCacheSupport localCacheSupport,
                                  RedisCacheSupport redisCacheSupport,
                                  TransactionHelper transactionHelper,
                                  CacheInvalidationPublisher cacheInvalidationPublisher) {
        this.localCacheSupport = localCacheSupport;
        this.redisCacheSupport = redisCacheSupport;
        this.transactionHelper = transactionHelper;
        this.cacheInvalidationPublisher = cacheInvalidationPublisher;
    }

    public <T> RedisCacheSupport.CacheValue<T> getJson(String key, Class<T> clazz) {
        RedisCacheSupport.CacheValue<T> local = localCacheSupport.getJson(key, clazz);
        if (local.found()) {
            return local;
        }
        RedisCacheSupport.CacheValue<T> redis = redisCacheSupport.getJson(key, clazz);
        if (redis.found()) {
            warmLocal(key, redis);
        }
        return redis;
    }

    public <T> RedisCacheSupport.CacheValue<T> getJson(String key, TypeReference<T> typeReference) {
        RedisCacheSupport.CacheValue<T> local = localCacheSupport.getJson(key, typeReference);
        if (local.found()) {
            return local;
        }
        RedisCacheSupport.CacheValue<T> redis = redisCacheSupport.getJson(key, typeReference);
        if (redis.found()) {
            warmLocal(key, redis);
        }
        return redis;
    }

    public String getString(String key) {
        String local = localCacheSupport.getString(key);
        if (local != null) {
            return local;
        }
        String redis = redisCacheSupport.getString(key);
        if (redis != null) {
            localCacheSupport.setString(key, redis, localTtlSec(key));
        }
        return redis;
    }

    public List<String> multiGet(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return List.of();
        }
        List<String> result = new ArrayList<>(keys.size());
        List<String> missingKeys = new ArrayList<>();
        List<Integer> missingIndexes = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            String raw = localCacheSupport.getString(keys.get(i));
            if (raw == null) {
                missingKeys.add(keys.get(i));
                missingIndexes.add(i);
                result.add(null);
            } else {
                result.add(raw);
            }
        }
        if (missingKeys.isEmpty()) {
            return result;
        }
        List<String> redisValues = redisCacheSupport.multiGet(missingKeys);
        for (int i = 0; i < missingKeys.size(); i++) {
            String raw = i < redisValues.size() ? redisValues.get(i) : null;
            if (raw != null) {
                localCacheSupport.setString(missingKeys.get(i), raw, localTtlSec(missingKeys.get(i)));
            }
            result.set(missingIndexes.get(i), raw);
        }
        return result;
    }

    public void setJson(String key, Object value, long ttlSec) {
        redisCacheSupport.setJson(key, value, ttlSec);
        localCacheSupport.setJson(key, value, localTtlSec(key));
    }

    public void setString(String key, String value, long ttlSec) {
        redisCacheSupport.setString(key, value, ttlSec);
        localCacheSupport.setString(key, value, localTtlSec(key));
    }

    public void cacheNull(String key, long ttlSec) {
        redisCacheSupport.cacheNull(key, ttlSec);
        localCacheSupport.cacheNull(key, localTtlSec(key));
    }

    public void sharedEvict(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        transactionHelper.afterCommitOrNow(() -> {
            redisCacheSupport.safeDelete(key);
            localCacheSupport.safeDelete(key);
            cacheInvalidationPublisher.publishKey(key);
        });
    }

    public void sharedEvictByPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return;
        }
        transactionHelper.afterCommitOrNow(() -> {
            redisCacheSupport.deleteByPrefix(prefix);
            localCacheSupport.deleteByPrefix(prefix);
            cacheInvalidationPublisher.publishPrefix(prefix);
        });
    }

    public void localEvict(String key) {
        localCacheSupport.safeDelete(key);
    }

    public void localEvictByPrefix(String prefix) {
        localCacheSupport.deleteByPrefix(prefix);
    }

    @Deprecated
    public void safeDelete(String key) {
        sharedEvict(key);
    }

    @Deprecated
    public void safeDelete(Collection<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return;
        }
        transactionHelper.afterCommitOrNow(() -> {
            localCacheSupport.safeDelete(keys);
            redisCacheSupport.safeDelete(keys);
            keys.forEach(cacheInvalidationPublisher::publishKey);
        });
    }

    @Deprecated
    public void deleteByPrefix(String prefix) {
        sharedEvictByPrefix(prefix);
    }

    public boolean tryLock(String key, String token, long ttlSec) {
        return redisCacheSupport.tryLock(key, token, ttlSec);
    }

    public String newLockToken() {
        return redisCacheSupport.newLockToken();
    }

    public void unlock(String key, String token) {
        redisCacheSupport.unlock(key, token);
    }

    public long ttlWithJitter(long baseSec, long maxJitterSec) {
        return redisCacheSupport.ttlWithJitter(baseSec, maxJitterSec);
    }

    private <T> void warmLocal(String key, RedisCacheSupport.CacheValue<T> cacheValue) {
        if (cacheValue.nullValue()) {
            localCacheSupport.cacheNull(key, localTtlSec(key));
            return;
        }
        localCacheSupport.setJson(key, cacheValue.value(), localTtlSec(key));
    }

    private long localTtlSec(String key) {
        if (RedisConstant.APP_BANNER_ACTIVE_KEY.equals(key)) {
            return RedisConstant.LOCAL_CACHE_BANNER_TTL_SEC;
        }
        if (key != null && key.startsWith(RedisConstant.APP_NOTICE_PUBLISHED_PREFIX)) {
            return RedisConstant.LOCAL_CACHE_NOTICE_TTL_SEC;
        }
        if (key != null && key.startsWith(RedisConstant.APP_SYS_CONFIG_PREFIX)) {
            return RedisConstant.LOCAL_CACHE_SYS_CONFIG_TTL_SEC;
        }
        if (key != null && key.startsWith(RedisConstant.APP_COURSE_LIST_PREFIX)) {
            return RedisConstant.LOCAL_CACHE_COURSE_LIST_TTL_SEC;
        }
        if (RedisConstant.APP_PLAN_LIST_ACTIVE_KEY.equals(key)) {
            return RedisConstant.LOCAL_CACHE_PLAN_LIST_TTL_SEC;
        }
        if (key != null && key.startsWith(RedisConstant.APP_PLAN_DETAIL_STATIC_PREFIX)) {
            return RedisConstant.LOCAL_CACHE_PLAN_DETAIL_TTL_SEC;
        }
        return RedisConstant.CACHE_NULL_TTL_SEC;
    }
}
