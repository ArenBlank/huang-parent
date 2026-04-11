package com.huang.common.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CacheInvalidationPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheInvalidationProperties properties;
    private final String instanceId;

    public CacheInvalidationPublisher(StringRedisTemplate stringRedisTemplate,
                                      ObjectMapper objectMapper,
                                      CacheInvalidationProperties properties,
                                      @Value("${app.instance-id:${spring.application.name}:${server.port}}") String instanceId) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.instanceId = instanceId;
    }

    public void publishKey(String key) {
        publish(CacheInvalidationAction.KEY, key);
    }

    public void publishPrefix(String prefix) {
        publish(CacheInvalidationAction.PREFIX, prefix);
    }

    private void publish(CacheInvalidationAction action, String target) {
        if (target == null || target.isBlank()) {
            return;
        }
        try {
            CacheInvalidationMessage message = CacheInvalidationMessage.builder()
                    .action(action)
                    .target(target)
                    .sourceInstanceId(instanceId)
                    .publishedAt(System.currentTimeMillis())
                    .build();
            String payload = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(properties.getChannel(), payload);
            log.debug("published cache invalidation, action={}, target={}, channel={}", action, target, properties.getChannel());
        } catch (Exception e) {
            log.warn("publish cache invalidation failed, action={}, target={}", action, target, e);
        }
    }
}
