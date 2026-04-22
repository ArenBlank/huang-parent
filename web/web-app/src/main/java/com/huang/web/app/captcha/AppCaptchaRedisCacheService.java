package com.huang.web.app.captcha;

import com.anji.captcha.service.CaptchaCacheService;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

public class AppCaptchaRedisCacheService implements CaptchaCacheService {

    private static final String CACHE_TYPE = "redis";
    private static final String KEY_PREFIX = "captcha:aj:";

    @Override
    public void set(String key, String value, long expiresInSeconds) {
        redis().opsForValue().set(resolveKey(key), value, Duration.ofSeconds(expiresInSeconds));
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redis().hasKey(resolveKey(key)));
    }

    @Override
    public void delete(String key) {
        redis().delete(resolveKey(key));
    }

    @Override
    public String get(String key) {
        return redis().opsForValue().get(resolveKey(key));
    }

    @Override
    public Long increment(String key, long value) {
        return redis().opsForValue().increment(resolveKey(key), value);
    }

    @Override
    public void setExpire(String key, long expiresInSeconds) {
        redis().expire(resolveKey(key), Duration.ofSeconds(expiresInSeconds));
    }

    @Override
    public String type() {
        return CACHE_TYPE;
    }

    private StringRedisTemplate redis() {
        return AppCaptchaRedisTemplateHolder.getStringRedisTemplate();
    }

    private String resolveKey(String key) {
        return KEY_PREFIX + key;
    }
}
