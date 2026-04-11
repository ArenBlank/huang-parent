package com.huang.common.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class CacheInvalidationSubscriber implements MessageListener {

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final MultiLevelCacheSupport multiLevelCacheSupport;
    private final String instanceId;

    public CacheInvalidationSubscriber(com.fasterxml.jackson.databind.ObjectMapper objectMapper,
                                       MultiLevelCacheSupport multiLevelCacheSupport,
                                       @Value("${app.instance-id:${spring.application.name}:${server.port}}") String instanceId) {
        this.objectMapper = objectMapper;
        this.multiLevelCacheSupport = multiLevelCacheSupport;
        this.instanceId = instanceId;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (message == null || message.getBody() == null || message.getBody().length == 0) {
            return;
        }
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            CacheInvalidationMessage invalidationMessage = objectMapper.readValue(payload, CacheInvalidationMessage.class);
            if (invalidationMessage.getAction() == null
                    || invalidationMessage.getTarget() == null
                    || invalidationMessage.getTarget().isBlank()) {
                log.debug("ignore empty cache invalidation payload, payload={}", payload);
                return;
            }
            if (instanceId.equals(invalidationMessage.getSourceInstanceId())) {
                log.debug("skip self cache invalidation message, action={}, target={}",
                        invalidationMessage.getAction(), invalidationMessage.getTarget());
                return;
            }
            if (invalidationMessage.getAction() == CacheInvalidationAction.KEY) {
                multiLevelCacheSupport.localEvict(invalidationMessage.getTarget());
            } else {
                multiLevelCacheSupport.localEvictByPrefix(invalidationMessage.getTarget());
            }
            log.debug("applied cache invalidation, action={}, target={}, sourceInstanceId={}",
                    invalidationMessage.getAction(), invalidationMessage.getTarget(), invalidationMessage.getSourceInstanceId());
        } catch (Exception e) {
            log.warn("consume cache invalidation message failed, payload={}", payload, e);
        }
    }
}
