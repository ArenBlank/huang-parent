package com.huang.common.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

public final class RedisLockLuaSupport {

    private static final DefaultRedisScript<Long> COMPARE_AND_DELETE_SCRIPT = new DefaultRedisScript<>(
            """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                      return redis.call('del', KEYS[1])
                    end
                    return 0
                    """,
            Long.class
    );

    private RedisLockLuaSupport() {
    }

    public static boolean compareAndDelete(StringRedisTemplate stringRedisTemplate, String key, String token) {
        Long deleted = stringRedisTemplate.execute(COMPARE_AND_DELETE_SCRIPT, List.of(key), token);
        return Long.valueOf(1L).equals(deleted);
    }
}
