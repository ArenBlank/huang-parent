package com.huang.web.admin.service.biz;

import com.huang.common.constant.RedisConstant;
import com.huang.common.constant.TaskRunConstant;
import com.huang.common.redis.LockAcquireResult;
import com.huang.common.redis.RedisGuardSupport;
import com.huang.model.entity.TaskRunLog;
import com.huang.web.admin.mapper.TaskRunLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTaskRunBizServiceTest {

    @Mock
    private TaskRunLogMapper taskRunLogMapper;

    @Mock
    private RedisGuardSupport redisGuardSupport;

    @Mock
    private AdminOpsBizService adminOpsBizService;

    @Mock
    private AdminPaymentCompensationBizService adminPaymentCompensationBizService;

    private AdminTaskRunBizService adminTaskRunBizService;

    @BeforeEach
    void setUp() {
        adminTaskRunBizService = new AdminTaskRunBizService(
                taskRunLogMapper,
                redisGuardSupport,
                adminOpsBizService,
                adminPaymentCompensationBizService,
                "8080"
        );
    }

    @Test
    void trigger_shouldRecordSuccessWhenTaskRunsNormally() {
        AtomicReference<TaskRunLog> store = new AtomicReference<>();
        when(redisGuardSupport.acquireLock(
                RedisConstant.taskLockKey(TaskRunConstant.TASK_PAYMENT_COMPENSATE),
                TaskRunConstant.lockTtlSec(TaskRunConstant.TASK_PAYMENT_COMPENSATE)
        ))
                .thenReturn(LockAcquireResult.acquired("lock-token"));
        when(adminPaymentCompensationBizService.repairPaidOrders(50)).thenReturn(3);
        when(taskRunLogMapper.insert(any(TaskRunLog.class))).thenAnswer(invocation -> {
            TaskRunLog log = invocation.getArgument(0);
            if (log.getId() == null) {
                log.setId(1L);
            }
            store.set(copy(log));
            return 1;
        });
        when(taskRunLogMapper.updateById(any(TaskRunLog.class))).thenAnswer(invocation -> {
            store.set(copy(invocation.getArgument(0)));
            return 1;
        });
        when(taskRunLogMapper.selectById(1L)).thenAnswer(invocation -> copy(store.get()));

        TaskRunLog result = adminTaskRunBizService.trigger(TaskRunConstant.TASK_PAYMENT_COMPENSATE);

        assertThat(result.getTaskCode()).isEqualTo(TaskRunConstant.TASK_PAYMENT_COMPENSATE);
        assertThat(result.getRunStatus()).isEqualTo(TaskRunConstant.STATUS_SUCCESS);
        assertThat(result.getAffectedCount()).isEqualTo(3);
        assertThat(result.getTriggerMode()).isEqualTo(TaskRunConstant.TRIGGER_MANUAL);
        verify(redisGuardSupport).releaseLock(RedisConstant.taskLockKey(TaskRunConstant.TASK_PAYMENT_COMPENSATE), "lock-token");
    }

    @Test
    void trigger_shouldRecordSkippedWhenTaskLockAlreadyHeld() {
        when(redisGuardSupport.acquireLock(
                RedisConstant.taskLockKey(TaskRunConstant.TASK_BOOKING_TIMEOUT_CLOSE),
                TaskRunConstant.lockTtlSec(TaskRunConstant.TASK_BOOKING_TIMEOUT_CLOSE)
        ))
                .thenReturn(LockAcquireResult.busy());
        when(taskRunLogMapper.insert(any(TaskRunLog.class))).thenAnswer(invocation -> {
            TaskRunLog log = invocation.getArgument(0);
            log.setId(2L);
            return 1;
        });

        TaskRunLog result = adminTaskRunBizService.trigger(TaskRunConstant.TASK_BOOKING_TIMEOUT_CLOSE);

        assertThat(result.getRunStatus()).isEqualTo(TaskRunConstant.STATUS_SKIPPED);
        assertThat(result.getAffectedCount()).isZero();
        assertThat(result.getMessage()).contains("task lock already held");
    }

    @Test
    void trigger_shouldRecordSkippedWhenTaskLockUnavailable() {
        when(redisGuardSupport.acquireLock(
                RedisConstant.taskLockKey(TaskRunConstant.TASK_BOOKING_TIMEOUT_CLOSE),
                TaskRunConstant.lockTtlSec(TaskRunConstant.TASK_BOOKING_TIMEOUT_CLOSE)
        ))
                .thenReturn(LockAcquireResult.degraded());
        when(taskRunLogMapper.insert(any(TaskRunLog.class))).thenAnswer(invocation -> {
            TaskRunLog log = invocation.getArgument(0);
            log.setId(3L);
            return 1;
        });

        TaskRunLog result = adminTaskRunBizService.trigger(TaskRunConstant.TASK_BOOKING_TIMEOUT_CLOSE);

        assertThat(result.getRunStatus()).isEqualTo(TaskRunConstant.STATUS_SKIPPED);
        assertThat(result.getAffectedCount()).isZero();
        assertThat(result.getMessage()).contains("task lock unavailable");
    }

    private TaskRunLog copy(TaskRunLog source) {
        TaskRunLog target = new TaskRunLog();
        target.setId(source.getId());
        target.setTaskCode(source.getTaskCode());
        target.setTaskName(source.getTaskName());
        target.setTriggerMode(source.getTriggerMode());
        target.setRunStatus(source.getRunStatus());
        target.setInstanceId(source.getInstanceId());
        target.setStartedAt(source.getStartedAt());
        target.setFinishedAt(source.getFinishedAt());
        target.setDurationMs(source.getDurationMs());
        target.setAffectedCount(source.getAffectedCount());
        target.setMessage(source.getMessage());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }
}
