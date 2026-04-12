package com.huang.common.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisGuardSupportTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisGuardSupport redisGuardSupport;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        redisGuardSupport = new RedisGuardSupport(stringRedisTemplate);
    }

    @Test
    void allowByFixedWindow_shouldSetExpireOnFirstRequest() {
        when(valueOperations.increment("limit:key")).thenReturn(1L);

        boolean allowed = redisGuardSupport.allowByFixedWindow("limit:key", 2, 30);

        assertThat(allowed).isTrue();
        verify(stringRedisTemplate).expire(eq("limit:key"), any());
    }

    @Test
    void allowByFixedWindow_shouldRejectWhenThresholdExceeded() {
        when(valueOperations.increment("limit:key")).thenReturn(3L);

        boolean allowed = redisGuardSupport.allowByFixedWindow("limit:key", 2, 30);

        assertThat(allowed).isFalse();
    }

    @Test
    void tryAcquireIdempotent_shouldReflectRedisSetIfAbsentResult() {
        when(valueOperations.setIfAbsent(eq("idem:key"), eq("1"), any())).thenReturn(Boolean.TRUE, Boolean.FALSE);

        boolean first = redisGuardSupport.tryAcquireIdempotent("idem:key", 5);
        boolean second = redisGuardSupport.tryAcquireIdempotent("idem:key", 5);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @Test
    void fixedWindowDecision_shouldReturnDegradedWhenRedisThrows() {
        when(valueOperations.increment("limit:key")).thenThrow(new RuntimeException("redis down"));

        RedisGuardSupport.GuardDecision decision = redisGuardSupport.fixedWindowDecision("limit:key", 2, 30);

        assertThat(decision).isEqualTo(RedisGuardSupport.GuardDecision.DEGRADED);
    }

    @Test
    void idempotentDecision_shouldReturnDegradedWhenRedisThrows() {
        when(valueOperations.setIfAbsent(eq("idem:key"), eq("1"), any())).thenThrow(new RuntimeException("redis down"));

        RedisGuardSupport.GuardDecision decision = redisGuardSupport.idempotentDecision("idem:key", 5);

        assertThat(decision).isEqualTo(RedisGuardSupport.GuardDecision.DEGRADED);
    }

    @Test
    void tryAcquireLock_andRelease_shouldUseTokenValue() {
        when(valueOperations.setIfAbsent(eq("lock:key"), any(), any())).thenReturn(Boolean.TRUE);
        when(valueOperations.get("lock:key")).thenAnswer(invocation -> currentLockToken);

        String token = redisGuardSupport.tryAcquireLock("lock:key", 5);
        currentLockToken = token;
        redisGuardSupport.releaseLock("lock:key", token);

        assertThat(token).isNotBlank();
        verify(stringRedisTemplate).delete("lock:key");
    }

    private String currentLockToken;
}
