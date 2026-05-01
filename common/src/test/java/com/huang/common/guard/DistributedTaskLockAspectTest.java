package com.huang.common.guard;

import com.huang.common.constant.RedisConstant;
import com.huang.common.redis.LockAcquireResult;
import com.huang.common.redis.RedisGuardSupport;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributedTaskLockAspectTest {

    @Mock
    private RedisGuardSupport redisGuardSupport;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private DistributedTaskLockAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new DistributedTaskLockAspect(redisGuardSupport, "fitness-app", "8081");
    }

    @Test
    void withTaskLock_shouldProceedAndReleaseWhenLockAcquired() throws Throwable {
        Method method = DemoTask.class.getDeclaredMethod("run");
        DistributedTaskLock annotation = method.getAnnotation(DistributedTaskLock.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn(null);
        when(redisGuardSupport.acquireLock(RedisConstant.taskScheduleLockKey("PAYMENT_COMPENSATE"), 300L))
                .thenReturn(LockAcquireResult.acquired("lock-token"));

        Object result = aspect.withTaskLock(joinPoint, annotation);

        assertThat(result).isNull();
        verify(joinPoint).proceed();
        verify(redisGuardSupport).releaseLock(RedisConstant.taskScheduleLockKey("PAYMENT_COMPENSATE"), "lock-token");
    }

    @Test
    void withTaskLock_shouldSkipWhenLockAlreadyHeld() throws Throwable {
        Method method = DemoTask.class.getDeclaredMethod("run");
        DistributedTaskLock annotation = method.getAnnotation(DistributedTaskLock.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(redisGuardSupport.acquireLock(RedisConstant.taskScheduleLockKey("PAYMENT_COMPENSATE"), 300L))
                .thenReturn(LockAcquireResult.busy());

        Object result = aspect.withTaskLock(joinPoint, annotation);

        assertThat(result).isNull();
        verify(joinPoint, never()).proceed();
        verify(redisGuardSupport, never()).releaseLock(any(), any());
    }

    @Test
    void withTaskLock_shouldFailCloseWhenRedisUnavailableByDefault() throws Throwable {
        Method method = DemoTask.class.getDeclaredMethod("run");
        DistributedTaskLock annotation = method.getAnnotation(DistributedTaskLock.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(redisGuardSupport.acquireLock(RedisConstant.taskScheduleLockKey("PAYMENT_COMPENSATE"), 300L))
                .thenReturn(LockAcquireResult.degraded());

        Object result = aspect.withTaskLock(joinPoint, annotation);

        assertThat(result).isNull();
        verify(joinPoint, never()).proceed();
        verify(redisGuardSupport, never()).releaseLock(any(), any());
    }

    @Test
    void withTaskLock_shouldFailOpenWhenRedisUnavailableAndConfigured() throws Throwable {
        Method method = FailOpenDemoTask.class.getDeclaredMethod("runAndReturn");
        DistributedTaskLock annotation = method.getAnnotation(DistributedTaskLock.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn(7);
        when(redisGuardSupport.acquireLock(RedisConstant.taskScheduleLockKey("BOOKING_TIMEOUT_CLOSE"), 180L))
                .thenReturn(LockAcquireResult.degraded());

        Object result = aspect.withTaskLock(joinPoint, annotation);

        assertThat(result).isEqualTo(7);
        verify(joinPoint).proceed();
    }

    @SuppressWarnings("unused")
    private static class DemoTask {

        @DistributedTaskLock(taskCode = "PAYMENT_COMPENSATE", ttlSec = 300)
        public void run() {
        }
    }

    @SuppressWarnings("unused")
    private static class FailOpenDemoTask {

        @DistributedTaskLock(taskCode = "BOOKING_TIMEOUT_CLOSE", ttlSec = 180, failOpen = true)
        public int runAndReturn() {
            return 7;
        }
    }
}
