package com.huang.common.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisCacheSupportTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private RedisCacheSupport redisCacheSupport;

    @BeforeEach
    void setUp() {
        redisCacheSupport = new RedisCacheSupport(stringRedisTemplate, new ObjectMapper());
    }

    @Test
    void deleteByPrefix_shouldUseScanInsteadOfKeys() {
        Set<String> keys = new LinkedHashSet<>();
        keys.add("app:notice:published:1");
        keys.add("app:notice:published:2");
        when(stringRedisTemplate.execute(org.mockito.ArgumentMatchers.<RedisCallback<Set<String>>>any())).thenReturn(keys);

        redisCacheSupport.deleteByPrefix("app:notice:published:");

        verify(stringRedisTemplate).execute(org.mockito.ArgumentMatchers.<RedisCallback<Set<String>>>any());
        verify(stringRedisTemplate).delete(eq(keys));
        verify(stringRedisTemplate, never()).keys(any());
    }

    @Test
    void deleteByPrefix_shouldSkipDeleteWhenScanReturnsEmpty() {
        when(stringRedisTemplate.execute(org.mockito.ArgumentMatchers.<RedisCallback<Set<String>>>any())).thenReturn(Set.of());

        redisCacheSupport.deleteByPrefix("app:notice:published:");

        verify(stringRedisTemplate).execute(org.mockito.ArgumentMatchers.<RedisCallback<Set<String>>>any());
        verify(stringRedisTemplate, never()).delete(any(java.util.Collection.class));
        verify(stringRedisTemplate, never()).keys(any());
    }
}
