package com.huang.web.app.service.biz;

import com.huang.common.constant.RedisConstant;
import com.huang.common.constant.TaskRunConstant;
import com.huang.common.redis.RedisGuardSupport;
import com.huang.model.entity.TaskRunLog;
import com.huang.web.app.mapper.TaskRunLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class AppTaskRunBizService {

    private static final Logger log = LoggerFactory.getLogger(AppTaskRunBizService.class);

    private final TaskRunLogMapper taskRunLogMapper;
    private final RedisGuardSupport redisGuardSupport;
    private final String instanceId;

    public AppTaskRunBizService(TaskRunLogMapper taskRunLogMapper,
                                RedisGuardSupport redisGuardSupport,
                                @Value("${server.port:8081}") String port) {
        this.taskRunLogMapper = taskRunLogMapper;
        this.redisGuardSupport = redisGuardSupport;
        this.instanceId = "web-app:" + port;
    }

    public int executeTask(String taskCode, String taskName, String triggerMode, TaskRunner runner) {
        String lockKey = RedisConstant.taskLockKey(taskCode);
        String lockToken = redisGuardSupport.tryAcquireLock(lockKey, RedisConstant.TASK_LOCK_TTL_SEC);
        if (lockToken == null) {
            recordSkip(taskCode, taskName, triggerMode, "task lock already held");
            return 0;
        }

        TaskRunLog logRow = new TaskRunLog();
        LocalDateTime startedAt = LocalDateTime.now();
        logRow.setTaskCode(taskCode);
        logRow.setTaskName(taskName);
        logRow.setTriggerMode(triggerMode);
        logRow.setRunStatus(TaskRunConstant.STATUS_RUNNING);
        logRow.setInstanceId(instanceId);
        logRow.setStartedAt(startedAt);
        taskRunLogMapper.insert(logRow);

        try {
            int affected = runner.run();
            finish(logRow, TaskRunConstant.STATUS_SUCCESS, affected, "ok", startedAt);
            return affected;
        } catch (Exception ex) {
            finish(logRow, TaskRunConstant.STATUS_FAILED, 0, trim(ex.getMessage()), startedAt);
            log.warn("task run failed, taskCode={}, triggerMode={}", taskCode, triggerMode, ex);
            throw ex;
        } finally {
            redisGuardSupport.releaseLock(lockKey, lockToken);
        }
    }

    private void recordSkip(String taskCode, String taskName, String triggerMode, String message) {
        TaskRunLog logRow = new TaskRunLog();
        LocalDateTime now = LocalDateTime.now();
        logRow.setTaskCode(taskCode);
        logRow.setTaskName(taskName);
        logRow.setTriggerMode(triggerMode);
        logRow.setRunStatus(TaskRunConstant.STATUS_SKIPPED);
        logRow.setInstanceId(instanceId);
        logRow.setStartedAt(now);
        logRow.setFinishedAt(now);
        logRow.setDurationMs(0L);
        logRow.setAffectedCount(0);
        logRow.setMessage(trim(message));
        taskRunLogMapper.insert(logRow);
    }

    private void finish(TaskRunLog logRow,
                        String status,
                        int affectedCount,
                        String message,
                        LocalDateTime startedAt) {
        LocalDateTime finishedAt = LocalDateTime.now();
        logRow.setRunStatus(status);
        logRow.setFinishedAt(finishedAt);
        logRow.setDurationMs(ChronoUnit.MILLIS.between(startedAt, finishedAt));
        logRow.setAffectedCount(affectedCount);
        logRow.setMessage(trim(message));
        taskRunLogMapper.updateById(logRow);
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    @FunctionalInterface
    public interface TaskRunner {
        int run();
    }
}
