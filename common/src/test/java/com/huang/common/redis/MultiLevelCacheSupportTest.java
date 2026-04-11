package com.huang.common.redis;

import com.huang.common.constant.RedisConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultiLevelCacheSupportTest {

    @Mock
    private LocalCacheSupport localCacheSupport;

    @Mock
    private RedisCacheSupport redisCacheSupport;

    @Mock
    private CacheInvalidationPublisher cacheInvalidationPublisher;

    private TransactionHelper transactionHelper;
    private MultiLevelCacheSupport multiLevelCacheSupport;

    @BeforeEach
    void setUp() {
        transactionHelper = new TransactionHelper();
        multiLevelCacheSupport = new MultiLevelCacheSupport(
                localCacheSupport,
                redisCacheSupport,
                transactionHelper,
                cacheInvalidationPublisher
        );
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void getString_shouldReturnLocalValueFirst() {
        when(localCacheSupport.getString("app:banner:active")).thenReturn("cached");

        String result = multiLevelCacheSupport.getString("app:banner:active");

        assertThat(result).isEqualTo("cached");
        verify(redisCacheSupport, never()).getString(any());
    }

    @Test
    void getJson_shouldWarmLocalCacheWhenRedisHits() {
        when(localCacheSupport.getJson(eq(RedisConstant.APP_PLAN_LIST_ACTIVE_KEY), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(RedisCacheSupport.CacheValue.miss());
        when(redisCacheSupport.getJson(eq(RedisConstant.APP_PLAN_LIST_ACTIVE_KEY), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(RedisCacheSupport.CacheValue.hit("payload"));

        var result = multiLevelCacheSupport.getJson(
                RedisConstant.APP_PLAN_LIST_ACTIVE_KEY,
                new com.fasterxml.jackson.core.type.TypeReference<String>() {}
        );

        assertThat(result.found()).isTrue();
        assertThat(result.value()).isEqualTo("payload");
        verify(localCacheSupport).setJson(RedisConstant.APP_PLAN_LIST_ACTIVE_KEY, "payload", RedisConstant.LOCAL_CACHE_PLAN_LIST_TTL_SEC);
    }

    @Test
    void cacheNull_shouldWriteRedisAndLocal() {
        multiLevelCacheSupport.cacheNull(RedisConstant.APP_BANNER_ACTIVE_KEY, 60);

        verify(redisCacheSupport).cacheNull(RedisConstant.APP_BANNER_ACTIVE_KEY, 60);
        verify(localCacheSupport).cacheNull(RedisConstant.APP_BANNER_ACTIVE_KEY, RedisConstant.LOCAL_CACHE_BANNER_TTL_SEC);
    }

    @Test
    void localEvictByPrefix_shouldOnlyDeleteLocal() {
        multiLevelCacheSupport.localEvictByPrefix(RedisConstant.APP_NOTICE_PUBLISHED_PREFIX);

        verify(localCacheSupport).deleteByPrefix(RedisConstant.APP_NOTICE_PUBLISHED_PREFIX);
        verifyNoInteractions(redisCacheSupport, cacheInvalidationPublisher);
    }

    @Test
    void sharedEvict_shouldExecuteAfterCommitOnCommit() {
        beginTransaction();

        multiLevelCacheSupport.sharedEvict(RedisConstant.APP_BANNER_ACTIVE_KEY);

        verifyNoInteractions(localCacheSupport, redisCacheSupport, cacheInvalidationPublisher);

        commitTransaction();

        verify(redisCacheSupport).safeDelete(RedisConstant.APP_BANNER_ACTIVE_KEY);
        verify(localCacheSupport).safeDelete(RedisConstant.APP_BANNER_ACTIVE_KEY);
        verify(cacheInvalidationPublisher).publishKey(RedisConstant.APP_BANNER_ACTIVE_KEY);
    }

    @Test
    void sharedEvict_shouldNotRunWhenTransactionRollsBack() {
        beginTransaction();

        multiLevelCacheSupport.sharedEvict(RedisConstant.APP_BANNER_ACTIVE_KEY);

        rollbackTransaction();

        verifyNoInteractions(localCacheSupport, redisCacheSupport, cacheInvalidationPublisher);
    }

    private void beginTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    private void commitTransaction() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        rollbackTransaction();
    }

    private void rollbackTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }
}
