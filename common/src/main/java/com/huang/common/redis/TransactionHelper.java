package com.huang.common.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
public class TransactionHelper {

    public void afterCommitOrNow(Runnable task) {
        if (task == null) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runSafely(task);
                }
            });
            return;
        }
        runSafely(task);
    }

    private void runSafely(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.warn("deferred task execution failed", e);
        }
    }
}
