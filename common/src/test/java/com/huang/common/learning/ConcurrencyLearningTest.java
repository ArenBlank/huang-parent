package com.huang.common.learning;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrencyLearningTest {

    @Test
    void shouldCompareNoGuardRedisStyleGuardAndUniqueIndexFallback() throws InterruptedException {
        LearningResult noGuard = runScenario(20, GuardMode.NO_GUARD);
        LearningResult redisGuard = runScenario(20, GuardMode.REDIS_STYLE_IDEMPOTENT);
        LearningResult uniqueFallback = runScenario(20, GuardMode.DB_UNIQUE_FALLBACK);

        assertThat(noGuard.successCount()).isGreaterThan(1);
        assertThat(redisGuard.successCount()).isEqualTo(1);
        assertThat(redisGuard.dbAttemptCount()).isEqualTo(1);

        assertThat(uniqueFallback.successCount()).isEqualTo(1);
        assertThat(uniqueFallback.dbAttemptCount()).isEqualTo(20);
        assertThat(uniqueFallback.dbDuplicateRejectCount()).isEqualTo(19);
    }

    private LearningResult runScenario(int concurrency, GuardMode mode) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(concurrency);
        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(concurrency);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger dbAttemptCount = new AtomicInteger();
        AtomicInteger dbDuplicateRejectCount = new AtomicInteger();

        Set<String> redisKeys = ConcurrentHashMap.newKeySet();
        Set<String> uniqueRows = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < concurrency; i++) {
            executorService.submit(() -> {
                ready.countDown();
                await(start);

                String requestKey = "user:7:schedule:3";
                if (mode == GuardMode.REDIS_STYLE_IDEMPOTENT && !redisKeys.add(requestKey)) {
                    done.countDown();
                    return;
                }

                dbAttemptCount.incrementAndGet();
                if (mode == GuardMode.DB_UNIQUE_FALLBACK) {
                    boolean inserted = uniqueRows.add(requestKey);
                    if (!inserted) {
                        dbDuplicateRejectCount.incrementAndGet();
                        done.countDown();
                        return;
                    }
                }

                successCount.incrementAndGet();
                done.countDown();
            });
        }

        ready.await(3, TimeUnit.SECONDS);
        start.countDown();
        done.await(5, TimeUnit.SECONDS);
        executorService.shutdownNow();

        return new LearningResult(
                mode,
                successCount.get(),
                dbAttemptCount.get(),
                dbDuplicateRejectCount.get()
        );
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private enum GuardMode {
        NO_GUARD,
        REDIS_STYLE_IDEMPOTENT,
        DB_UNIQUE_FALLBACK
    }

    private record LearningResult(
            GuardMode guardMode,
            int successCount,
            int dbAttemptCount,
            int dbDuplicateRejectCount
    ) {
    }
}
