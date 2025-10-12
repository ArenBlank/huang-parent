package com.huang.web.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class RedisConnectionTest {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void testRedisConnection() {
        redisTemplate.opsForValue().set("spring-test", "success");
        String result = redisTemplate.opsForValue().get("spring-test");
        System.out.println("Redis test result: " + result);
        assertEquals("success", result);
    }
}