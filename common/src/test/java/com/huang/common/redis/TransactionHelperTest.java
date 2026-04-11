package com.huang.common.redis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionHelperTest {

    private final TransactionHelper transactionHelper = new TransactionHelper();

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void afterCommitOrNow_shouldRunImmediatelyWithoutTransaction() {
        AtomicInteger counter = new AtomicInteger();

        transactionHelper.afterCommitOrNow(counter::incrementAndGet);

        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void afterCommitOrNow_shouldRunAfterCommitWhenTransactionActive() {
        AtomicInteger counter = new AtomicInteger();
        beginTransaction();

        transactionHelper.afterCommitOrNow(counter::incrementAndGet);

        assertThat(counter.get()).isZero();
        triggerAfterCommit();

        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void afterCommitOrNow_shouldNotRunWhenTransactionRollsBack() {
        AtomicInteger counter = new AtomicInteger();
        beginTransaction();

        transactionHelper.afterCommitOrNow(counter::incrementAndGet);
        tearDown();

        assertThat(counter.get()).isZero();
    }

    private void beginTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    private void triggerAfterCommit() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        tearDown();
    }
}
