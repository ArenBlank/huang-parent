package com.huang.web.app.service.biz.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huang.model.entity.User;
import com.huang.web.app.service.core.UserCoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppAuthCacheServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private UserCoreService userCoreService;

    private AppAuthCacheService appAuthCacheService;

    @BeforeEach
    void setUp() {
        appAuthCacheService = new AppAuthCacheService(stringRedisTemplate, new ObjectMapper(), userCoreService);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getOrLoad_shouldReturnCachedSnapshotWhenJsonContainsUnknownFields() {
        when(valueOperations.get(AppAuthCacheService.authKey(11L)))
                .thenReturn("{\"status\":1,\"tokenVersion\":4,\"ignored\":\"x\"}");

        AppAuthSnapshot snapshot = appAuthCacheService.getOrLoad(11L);

        assertNotNull(snapshot);
        assertEquals(1, snapshot.status());
        assertEquals(4, snapshot.tokenVersion());
        verify(userCoreService, never()).getById(any());
    }

    @Test
    void getOrLoad_shouldLoadFromDbAndCacheOnMiss() {
        User user = new User();
        user.setId(12L);
        user.setStatus(1);
        user.setTokenVersion(-9);
        when(valueOperations.get(AppAuthCacheService.authKey(12L))).thenReturn(null);
        when(userCoreService.getById(12L)).thenReturn(user);

        AppAuthSnapshot snapshot = appAuthCacheService.getOrLoad(12L);

        assertNotNull(snapshot);
        assertEquals(1, snapshot.status());
        assertEquals(0, snapshot.tokenVersion());
        verify(valueOperations).set(eq(AppAuthCacheService.authKey(12L)), anyString(), eq(Duration.ofSeconds(7200L)));
    }

    @Test
    void evict_shouldDeleteRedisKey() {
        appAuthCacheService.evict(13L);

        verify(stringRedisTemplate).delete(AppAuthCacheService.authKey(13L));
    }
}
