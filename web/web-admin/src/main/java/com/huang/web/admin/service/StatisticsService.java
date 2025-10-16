package com.huang.web.admin.service;

import com.huang.web.admin.vo.statistics.AccountStatisticsVO;

/**
 * 统计服务接口
 */
public interface StatisticsService {

    /**
     * 获取账号统计信息(模拟数据)
     */
    AccountStatisticsVO getAccountStatistics();

    /**
     * 执行课程统计任务
     */
    void executeCourseStatisticsTask();

    /**
     * 执行教练工作统计任务
     */
    void executeCoachStatisticsTask();

    /**
     * 执行课程状态检查任务
     */
    void executeCourseStatusCheckTask();

    /**
     * 清理过期临时文件
     */
    void cleanExpiredTempFiles();
}
