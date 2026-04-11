package com.huang.common.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheInvalidationSubscriberTest {

    @Mock
    private MultiLevelCacheSupport multiLevelCacheSupport;

    private ObjectMapper objectMapper;
    private CacheInvalidationSubscriber subscriber;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        subscriber = new CacheInvalidationSubscriber(objectMapper, multiLevelCacheSupport, "fitness-app:8081");
    }

    @Test
    void onMessage_shouldSkipSelfMessage() throws Exception {
        Message message = redisMessage(CacheInvalidationMessage.builder()
                .action(CacheInvalidationAction.KEY)
                .target("app:banner:active")
                .sourceInstanceId("fitness-app:8081")
                .publishedAt(System.currentTimeMillis())
                .build());

        subscriber.onMessage(message, null);

        verifyNoInteractions(multiLevelCacheSupport);
    }

    @Test
    void onMessage_shouldEvictLocalKeyForKeyAction() throws Exception {
        Message message = redisMessage(CacheInvalidationMessage.builder()
                .action(CacheInvalidationAction.KEY)
                .target("app:banner:active")
                .sourceInstanceId("fitness-admin:8080")
                .publishedAt(System.currentTimeMillis())
                .build());

        subscriber.onMessage(message, null);

        verify(multiLevelCacheSupport).localEvict("app:banner:active");
        verify(multiLevelCacheSupport, never()).localEvictByPrefix(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void onMessage_shouldEvictLocalPrefixForPrefixAction() throws Exception {
        Message message = redisMessage(CacheInvalidationMessage.builder()
                .action(CacheInvalidationAction.PREFIX)
                .target("app:notice:published:")
                .sourceInstanceId("fitness-admin:8080")
                .publishedAt(System.currentTimeMillis())
                .build());

        subscriber.onMessage(message, null);

        verify(multiLevelCacheSupport).localEvictByPrefix("app:notice:published:");
        verify(multiLevelCacheSupport, never()).localEvict(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void onMessage_shouldIgnoreInvalidPayload() {
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn("not-json".getBytes(StandardCharsets.UTF_8));

        subscriber.onMessage(message, null);

        verifyNoInteractions(multiLevelCacheSupport);
    }

    private Message redisMessage(CacheInvalidationMessage payload) throws Exception {
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(objectMapper.writeValueAsBytes(payload));
        return message;
    }
}
