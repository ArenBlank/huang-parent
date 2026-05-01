package com.huang.common.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.anyList;
import org.springframework.data.redis.core.script.RedisScript;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class RedisGuardSupportTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisGuardSupport redisGuardSupport;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
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
    void acquireLock_shouldReturnAcquiredDecisionWhenRedisSucceeds() {
        when(valueOperations.setIfAbsent(eq("lock:key"), any(), any())).thenReturn(Boolean.TRUE);

        LockAcquireResult result = redisGuardSupport.acquireLock("lock:key", 5);

        assertThat(result.isAcquired()).isTrue();
        assertThat(result.token()).isNotBlank();
    }

    @Test
    void acquireLock_shouldReturnBusyDecisionWhenRedisLockIsHeld() {
        when(valueOperations.setIfAbsent(eq("lock:key"), any(), any())).thenReturn(Boolean.FALSE);

        LockAcquireResult result = redisGuardSupport.acquireLock("lock:key", 5);

        assertThat(result.isBusy()).isTrue();
        assertThat(result.token()).isNull();
    }

    @Test
    void acquireLock_shouldReturnDegradedDecisionWhenRedisThrows() {
        when(valueOperations.setIfAbsent(eq("lock:key"), any(), any())).thenThrow(new RuntimeException("redis down"));

        LockAcquireResult result = redisGuardSupport.acquireLock("lock:key", 5);

        assertThat(result.isDegraded()).isTrue();
        assertThat(result.token()).isEqualTo(RedisGuardSupport.NOOP_LOCK_TOKEN);
    }

    @Test
    void releaseLock_shouldUseLuaCompareAndDelete() {
        doReturn(1L).when(stringRedisTemplate)
                .execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any());

        redisGuardSupport.releaseLock("lock:key", "token-1");

        verify(stringRedisTemplate)
                .execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), eq(java.util.List.of("lock:key")), eq("token-1"));
    }

    @Test
    void releaseLock_shouldNotDeleteWhenTokenDoesNotMatch() {
        doReturn(0L).when(stringRedisTemplate)
                .execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any());

        redisGuardSupport.releaseLock("lock:key", "token-2");

        verify(stringRedisTemplate)
                .execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), eq(java.util.List.of("lock:key")), eq("token-2"));
    }
}
