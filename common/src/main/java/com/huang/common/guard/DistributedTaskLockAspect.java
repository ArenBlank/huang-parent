package com.huang.common.guard;

import com.huang.common.constant.RedisConstant;
import com.huang.common.redis.LockAcquireResult;
import com.huang.common.redis.RedisGuardSupport;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class DistributedTaskLockAspect {

    private final RedisGuardSupport redisGuardSupport;
    private final String instanceId;

    public DistributedTaskLockAspect(RedisGuardSupport redisGuardSupport,
                                     @Value("${spring.application.name:application}") String applicationName,
                                     @Value("${server.port:0}") String port) {
        this.redisGuardSupport = redisGuardSupport;
        this.instanceId = applicationName + ":" + port;
    }

    @Around("@annotation(distributedTaskLock)")
    public Object withTaskLock(ProceedingJoinPoint joinPoint, DistributedTaskLock distributedTaskLock) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String taskCode = StringUtils.hasText(distributedTaskLock.taskCode()) ? distributedTaskLock.taskCode().trim() : method.getName();
        String lockKey = RedisConstant.taskScheduleLockKey(taskCode);
        LockAcquireResult lockResult = redisGuardSupport.acquireLock(lockKey, distributedTaskLock.ttlSec());

        if (lockResult.isBusy()) {
            log.info("skip scheduled task because schedule lock is already held, taskCode={}, key={}, instanceId={}",
                    taskCode, lockKey, instanceId);
            return defaultValue(method.getReturnType());
        }

        if (lockResult.isDegraded()) {
            if (distributedTaskLock.failOpen()) {
                log.warn("schedule lock degraded to fail-open, taskCode={}, key={}, instanceId={}",
                        taskCode, lockKey, instanceId);
                return joinPoint.proceed();
            }
            log.warn("skip scheduled task because schedule lock is unavailable, taskCode={}, key={}, instanceId={}",
                    taskCode, lockKey, instanceId);
            return defaultValue(method.getReturnType());
        }

        try {
            return joinPoint.proceed();
        } finally {
            redisGuardSupport.releaseLock(lockKey, lockResult.token());
        }
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == null || Void.TYPE.equals(returnType) || !returnType.isPrimitive()) {
            return null;
        }
        if (Boolean.TYPE.equals(returnType)) {
            return false;
        }
        if (Character.TYPE.equals(returnType)) {
            return '\0';
        }
        if (Byte.TYPE.equals(returnType)) {
            return (byte) 0;
        }
        if (Short.TYPE.equals(returnType)) {
            return (short) 0;
        }
        if (Integer.TYPE.equals(returnType)) {
            return 0;
        }
        if (Long.TYPE.equals(returnType)) {
            return 0L;
        }
        if (Float.TYPE.equals(returnType)) {
            return 0F;
        }
        if (Double.TYPE.equals(returnType)) {
            return 0D;
        }
        return null;
    }
}
