package com.huang.web.admin.task;

import com.huang.web.admin.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 统计任务调度器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsScheduledTask {

    private final StatisticsService statisticsService;

    /**
     * 课程统计任务
     * 每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void courseStatisticsTask() {
        log.info("=== 开始执行课程统计定时任务 ===");
        try {
            statisticsService.executeCourseStatisticsTask();
            log.info("=== 课程统计定时任务执行成功 ===");
        } catch (Exception e) {
            log.error("课程统计定时任务执行失败", e);
        }
    }

    /**
     * 教练工作统计任务
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void coachStatisticsTask() {
        log.info("=== 开始执行教练工作统计定时任务 ===");
        try {
            statisticsService.executeCoachStatisticsTask();
            log.info("=== 教练工作统计定时任务执行成功 ===");
        } catch (Exception e) {
            log.error("教练工作统计定时任务执行失败", e);
        }
    }

    /**
     * 课程状态检查任务
     * 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void courseStatusCheckTask() {
        log.info("=== 开始执行课程状态检查定时任务 ===");
        try {
            statisticsService.executeCourseStatusCheckTask();
            log.info("=== 课程状态检查定时任务执行成功 ===");
        } catch (Exception e) {
            log.error("课程状态检查定时任务执行失败", e);
        }
    }

    /**
     * 清理过期临时文件任务
     * 每天凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredTempFilesTask() {
        log.info("=== 开始执行清理过期临时文件定时任务 ===");
        try {
            statisticsService.cleanExpiredTempFiles();
            log.info("=== 清理过期临时文件定时任务执行成功 ===");
        } catch (Exception e) {
            log.error("清理过期临时文件定时任务执行失败", e);
        }
    }
}
