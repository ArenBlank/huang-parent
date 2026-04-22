package com.huang.web.admin.captcha;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class AdminCaptchaRedisTemplateHolder implements ApplicationContextAware {

    private static volatile StringRedisTemplate stringRedisTemplate;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        stringRedisTemplate = applicationContext.getBean(StringRedisTemplate.class);
    }

    static StringRedisTemplate getStringRedisTemplate() {
        if (stringRedisTemplate == null) {
            throw new IllegalStateException("StringRedisTemplate is not ready for captcha cache");
        }
        return stringRedisTemplate;
    }
}
