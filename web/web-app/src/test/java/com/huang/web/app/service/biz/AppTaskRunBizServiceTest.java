package com.huang.web.app.service.biz;

import com.huang.common.constant.RedisConstant;
import com.huang.common.constant.TaskRunConstant;
import com.huang.common.redis.LockAcquireResult;
import com.huang.common.redis.RedisGuardSupport;
import com.huang.model.entity.TaskRunLog;
import com.huang.web.app.mapper.TaskRunLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppTaskRunBizServiceTest {

    @Mock
    private TaskRunLogMapper taskRunLogMapper;

    @Mock
    private RedisGuardSupport redisGuardSupport;

    private AppTaskRunBizService appTaskRunBizService;

    @BeforeEach
    void setUp() {
        appTaskRunBizService = new AppTaskRunBizService(taskRunLogMapper, redisGuardSupport, "8081");
    }

    @Test
    void executeTask_shouldUseTaskSpecificLockTtl() {
        when(redisGuardSupport.acquireLock(
                RedisConstant.taskLockKey(TaskRunConstant.TASK_PAYMENT_COMPENSATE),
                TaskRunConstant.lockTtlSec(TaskRunConstant.TASK_PAYMENT_COMPENSATE)
        )).thenReturn(LockAcquireResult.acquired("lock-token"));
        when(taskRunLogMapper.insert(any(TaskRunLog.class))).thenAnswer(invocation -> {
            TaskRunLog log = invocation.getArgument(0);
            log.setId(1L);
            return 1;
        });
        when(taskRunLogMapper.updateById(any(TaskRunLog.class))).thenReturn(1);

        int result = appTaskRunBizService.executeTask(
                TaskRunConstant.TASK_PAYMENT_COMPENSATE,
                TaskRunConstant.TASK_PAYMENT_COMPENSATE_NAME,
                TaskRunConstant.TRIGGER_SCHEDULED,
                () -> 2
        );

        assertThat(result).isEqualTo(2);
        verify(redisGuardSupport).releaseLock(RedisConstant.taskLockKey(TaskRunConstant.TASK_PAYMENT_COMPENSATE), "lock-token");
    }

    @Test
    void executeTask_shouldSkipWhenLockUnavailable() {
        when(redisGuardSupport.acquireLock(
                RedisConstant.taskLockKey(TaskRunConstant.TASK_PAYMENT_COMPENSATE),
                TaskRunConstant.lockTtlSec(TaskRunConstant.TASK_PAYMENT_COMPENSATE)
        )).thenReturn(LockAcquireResult.degraded());

        int result = appTaskRunBizService.executeTask(
                TaskRunConstant.TASK_PAYMENT_COMPENSATE,
                TaskRunConstant.TASK_PAYMENT_COMPENSATE_NAME,
                TaskRunConstant.TRIGGER_SCHEDULED,
                () -> 2
        );

        assertThat(result).isZero();
        var captor = forClass(TaskRunLog.class);
        verify(taskRunLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getRunStatus()).isEqualTo(TaskRunConstant.STATUS_SKIPPED);
        assertThat(captor.getValue().getMessage()).contains("task lock unavailable");
        verify(taskRunLogMapper, never()).updateById(any(TaskRunLog.class));
    }
}
