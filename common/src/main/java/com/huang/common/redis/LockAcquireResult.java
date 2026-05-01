package com.huang.common.redis;

public record LockAcquireResult(String token, RedisGuardSupport.LockDecision decision) {

    public static LockAcquireResult acquired(String token) {
        return new LockAcquireResult(token, RedisGuardSupport.LockDecision.ACQUIRED);
    }

    public static LockAcquireResult busy() {
        return new LockAcquireResult(null, RedisGuardSupport.LockDecision.BUSY);
    }

    public static LockAcquireResult degraded() {
        return new LockAcquireResult(RedisGuardSupport.NOOP_LOCK_TOKEN, RedisGuardSupport.LockDecision.DEGRADED);
    }

    public boolean isAcquired() {
        return decision == RedisGuardSupport.LockDecision.ACQUIRED;
    }

    public boolean isBusy() {
        return decision == RedisGuardSupport.LockDecision.BUSY;
    }

    public boolean isDegraded() {
        return decision == RedisGuardSupport.LockDecision.DEGRADED;
    }
}
