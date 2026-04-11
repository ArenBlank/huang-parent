package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.common.constant.RedisConstant;
import com.huang.common.constant.TaskRunConstant;
import com.huang.common.redis.RedisGuardSupport;
import com.huang.model.entity.TaskRunLog;
import com.huang.web.admin.mapper.TaskRunLogMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminTaskRunBizService {

    private final TaskRunLogMapper taskRunLogMapper;
    private final RedisGuardSupport redisGuardSupport;
    private final AdminOpsBizService adminOpsBizService;
    private final AdminPaymentCompensationBizService adminPaymentCompensationBizService;
    private final String instanceId;

    public AdminTaskRunBizService(TaskRunLogMapper taskRunLogMapper,
                                  RedisGuardSupport redisGuardSupport,
                                  AdminOpsBizService adminOpsBizService,
                                  AdminPaymentCompensationBizService adminPaymentCompensationBizService,
                                  @Value("${server.port:8080}") String port) {
        this.taskRunLogMapper = taskRunLogMapper;
        this.redisGuardSupport = redisGuardSupport;
        this.adminOpsBizService = adminOpsBizService;
        this.adminPaymentCompensationBizService = adminPaymentCompensationBizService;
        this.instanceId = "web-admin:" + port;
    }

    public Page<TaskRunLog> page(String taskCode,
                                 String runStatus,
                                 String triggerMode,
                                 Integer pageNo,
                                 Integer pageSize) {
        Page<TaskRunLog> page = new Page<>(
                pageNo == null || pageNo <= 0 ? 1 : pageNo,
                pageSize == null || pageSize <= 0 ? 20 : Math.min(pageSize, 100)
        );
        LambdaQueryWrapper<TaskRunLog> wrapper = new LambdaQueryWrapper<TaskRunLog>()
                .orderByDesc(TaskRunLog::getId);
        if (taskCode != null && !taskCode.isBlank()) {
            wrapper.eq(TaskRunLog::getTaskCode, taskCode.trim());
        }
        if (runStatus != null && !runStatus.isBlank()) {
            wrapper.eq(TaskRunLog::getRunStatus, runStatus.trim());
        }
        if (triggerMode != null && !triggerMode.isBlank()) {
            wrapper.eq(TaskRunLog::getTriggerMode, triggerMode.trim());
        }
        return taskRunLogMapper.selectPage(page, wrapper);
    }

    public Map<String, Object> summary() {
        List<TaskRunLog> latest = taskRunLogMapper.selectList(
                new LambdaQueryWrapper<TaskRunLog>()
                        .orderByDesc(TaskRunLog::getId)
                        .last("LIMIT 20")
        );
        Map<String, TaskRunLog> latestByTask = new LinkedHashMap<>();
        for (TaskRunLog row : latest) {
            latestByTask.putIfAbsent(row.getTaskCode(), row);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("latest", latest);
        summary.put("latestByTask", latestByTask);
        summary.put("taskCodes", List.of(
                TaskRunConstant.TASK_PAYMENT_COMPENSATE,
                TaskRunConstant.TASK_BOOKING_TIMEOUT_CLOSE
        ));
        return summary;
    }

    @Transactional(rollbackFor = Exception.class)
    public TaskRunLog trigger(String taskCode) {
        if (TaskRunConstant.TASK_PAYMENT_COMPENSATE.equalsIgnoreCase(taskCode)) {
            return executeTask(
                    TaskRunConstant.TASK_PAYMENT_COMPENSATE,
                    TaskRunConstant.TASK_PAYMENT_COMPENSATE_NAME,
                    TaskRunConstant.TRIGGER_MANUAL,
                    () -> adminPaymentCompensationBizService.repairPaidOrders(50)
            );
        }
        if (TaskRunConstant.TASK_BOOKING_TIMEOUT_CLOSE.equalsIgnoreCase(taskCode)) {
            return executeTask(
                    TaskRunConstant.TASK_BOOKING_TIMEOUT_CLOSE,
                    TaskRunConstant.TASK_BOOKING_TIMEOUT_CLOSE_NAME,
                    TaskRunConstant.TRIGGER_MANUAL,
                    () -> adminOpsBizService.closeTimeoutUnpaidBookings(30)
            );
        }
        throw new IllegalArgumentException("unsupported taskCode: " + taskCode);
    }

    @Transactional(rollbackFor = Exception.class)
    public int runScheduledBookingTimeoutClose(int timeoutMinutes) {
        TaskRunLog logRow = executeTask(
                TaskRunConstant.TASK_BOOKING_TIMEOUT_CLOSE,
                TaskRunConstant.TASK_BOOKING_TIMEOUT_CLOSE_NAME,
                TaskRunConstant.TRIGGER_SCHEDULED,
                () -> adminOpsBizService.closeTimeoutUnpaidBookings(timeoutMinutes)
        );
        return logRow.getAffectedCount() == null ? 0 : logRow.getAffectedCount();
    }

    private TaskRunLog executeTask(String taskCode,
                                   String taskName,
                                   String triggerMode,
                                   TaskRunner taskRunner) {
        String lockKey = RedisConstant.taskLockKey(taskCode);
        String lockToken = redisGuardSupport.tryAcquireLock(lockKey, RedisConstant.TASK_LOCK_TTL_SEC);
        if (lockToken == null) {
            return recordSkip(taskCode, taskName, triggerMode, "task lock already held");
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
            int affected = taskRunner.run();
            finish(logRow, TaskRunConstant.STATUS_SUCCESS, affected, "ok", startedAt);
            return taskRunLogMapper.selectById(logRow.getId());
        } catch (Exception ex) {
            finish(logRow, TaskRunConstant.STATUS_FAILED, 0, trim(ex.getMessage()), startedAt);
            throw ex;
        } finally {
            redisGuardSupport.releaseLock(lockKey, lockToken);
        }
    }

    private TaskRunLog recordSkip(String taskCode, String taskName, String triggerMode, String message) {
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
        return logRow;
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
    private interface TaskRunner {
        int run();
    }
}
