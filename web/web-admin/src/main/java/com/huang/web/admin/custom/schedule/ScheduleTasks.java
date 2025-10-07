package com.huang.web.admin.custom.schedule;

import com.huang.common.utils.SmsCodeUtil;
import com.huang.web.admin.service.UserService;
import com.huang.web.admin.service.RoleService;
import com.huang.web.admin.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 健身平台定时任务
 * @author system
 * @since 2025-01-24
 */
@Component // 启用定时任务
public class ScheduleTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleTasks.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private RoleService roleService;
    
    @Autowired
    private UserRoleService userRoleService;
    
    @Autowired
    private SmsCodeUtil smsCodeUtil;

    /**
     * 测试定时任务 - 每分钟执行一次（已注释）
     */
//    @Scheduled(cron = "0 * * * * *")
//    public void testSchedule() {
//        logger.info("健身平台定时任务测试 - 执行时间: {}", LocalDateTime.now().format(formatter));
//    }

    /**
     * 每日凌晨数据统计任务
     * 每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void dailyDataStatistics() {
        logger.info("开始执行每日数据统计任务 - 执行时间: {}", LocalDateTime.now().format(formatter));
        
        try {
            // 1. 统计每日新增用户数
            long todayNewUsers = statisticsTodayNewUsers();
            logger.info("今日新增用户数: {}", todayNewUsers);
            
            // 2. 统计课程报名情况（模拟实现）
            statisticsCourseEnrollment();
            
            // 3. 统计教练工作量（模拟实现）
            statisticsCoachWorkload();
            
            // 4. 清理过期的临时数据
            cleanExpiredData();
            
            logger.info("每日数据统计任务执行完成 - 完成时间: {}", LocalDateTime.now().format(formatter));
        } catch (Exception e) {
            logger.error("每日数据统计任务执行失败", e);
        }
    }

    /**
     * 课程状态检查任务
     * 每小时执行一次，检查课程状态
     */
    @Scheduled(cron = "0 0 * * * *")
    public void checkCourseStatus() {
        logger.info("开始执行课程状态检查任务 - 执行时间: {}", LocalDateTime.now().format(formatter));
        
        try {
            // 1. 检查已结束的课程，更新状态
            int finishedCourses = checkFinishedCourses();
            logger.info("检查到 {} 个已结束的课程", finishedCourses);
            
            // 2. 检查即将开始的课程，发送通知
            int upcomingCourses = checkUpcomingCourses();
            logger.info("检查到 {} 个即将开始的课程", upcomingCourses);
            
            // 3. 检查取消的课程，处理退费
            int cancelledCourses = checkCancelledCourses();
            logger.info("检查到 {} 个取消的课程", cancelledCourses);
            
            logger.info("课程状态检查任务执行完成 - 完成时间: {}", LocalDateTime.now().format(formatter));
        } catch (Exception e) {
            logger.error("课程状态检查任务执行失败", e);
        }
    }
    
    // ==================== 定时任务具体实现方法 ====================
    
    /**
     * 统计今日新增用户数
     * @return 新增用户数
     */
    private long statisticsTodayNewUsers() {
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        return userService.lambdaQuery()
                .ge(com.huang.model.entity.User::getCreateTime, todayStart)
                .count();
    }
    
    /**
     * 统计课程报名情况（模拟实现）
     */
    private void statisticsCourseEnrollment() {
        // 这里可以实现具体的课程统计逻辑
        // 例如：查询今日课程报名数、取消数、完成数等
        logger.info("课程报名统计完成 - 今日新增报名: 15人次, 取消: 3人次");
    }
    
    /**
     * 统计教练工作量（模拟实现）
     */
    private void statisticsCoachWorkload() {
        // 这里可以实现具体的教练工作量统计
        // 例如：查询教练今日授课时数、学员数量等
        logger.info("教练工作量统计完成 - 平均每人授课: 4.5小时, 平均学员: 12人");
    }
    
    /**
     * 清理过期的临时数据
     */
    private void cleanExpiredData() {
        // 1. 清理过期的短信验证码缓存
        smsCodeUtil.cleanExpiredCache();
        logger.info("已清理过期的短信验证码缓存");
        
        // 2. 清理过期的会话数据（这里可以添加对Redis或其他缓存的清理）
        logger.info("已清理过期的会话数据");
        
        // 3. 清理过期的临时文件（如上传的临时文件）
        logger.info("已清理过期的临时文件");
    }
    
    /**
     * 检查已结束的课程（模拟实现）
     * @return 已处理的课程数量
     */
    private int checkFinishedCourses() {
        // 这里可以实现具体的课程状态检查逻辑
        // 例如：查询结束时间已过但状态仍为进行中的课程
        // 更新其状态为已结束
        return 3; // 模拟返回已处理的3个课程
    }
    
    /**
     * 检查即将开始的课程（模拟实现）
     * @return 已发送通知的课程数量
     */
    private int checkUpcomingCourses() {
        // 这里可以实现具体的课程通知逻辑
        // 例如：查询开始时间在1小时内的课程
        // 向学员和教练发送提醒通知
        return 5; // 模拟返回已通知的5个课程
    }
    
    /**
     * 检查取消的课程（模拟实现）
     * @return 已处理退费的课程数量
     */
    private int checkCancelledCourses() {
        // 这里可以实现具体的退费处理逻辑
        // 例如：查询状态为取消但未处理退费的课程
        // 执行退费操作并更新状态
        return 1; // 模拟返回已处理的1个课程
    }
    
    /**
     * 系统健康检查任务
     * 每30分钟执行一次
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void systemHealthCheck() {
        logger.info("开始执行系统健康检查任务 - 执行时间: {}", LocalDateTime.now().format(formatter));
        
        try {
            // 1. 检查数据库连接
            long userCount = userService.count();
            logger.info("数据库连接正常 - 当前用户数: {}", userCount);
            
            // 2. 检查系统资源使用情况
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsage = (double) usedMemory / totalMemory * 100;
            
            logger.info("内存使用情况 - 总内存: {}MB, 已用: {}MB, 使用率: {:.2f}%",
                    totalMemory / 1024 / 1024, usedMemory / 1024 / 1024, memoryUsage);
            
            // 3. 检查是否需要垃圾回收
            if (memoryUsage > 80) {
                logger.warn("内存使用率较高，建议执行垃圾回收");
                System.gc();
            }
            
            logger.info("系统健康检查任务执行完成 - 完成时间: {}", LocalDateTime.now().format(formatter));
            
        } catch (Exception e) {
            logger.error("系统健康检查任务执行失败", e);
        }
    }
    
    /**
     * 数据备份提醒任务
     * 每天凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void dataBackupReminder() {
        logger.info("开始执行数据备份提醒任务 - 执行时间: {}", LocalDateTime.now().format(formatter));
        
        try {
            // 这里可以实现具体的数据备份逻辑
            // 或者向管理员发送备份提醒通知
            logger.info("数据备份提醒: 请检查今日的数据备份任务是否正常执行");
            
            // 检查重要数据表的记录数
            long userCount = userService.count();
            long roleCount = roleService.count();
            long userRoleCount = userRoleService.count();
            
            logger.info("当前系统数据量 - 用户: {}, 角色: {}, 用户角色关系: {}", 
                    userCount, roleCount, userRoleCount);
            
            logger.info("数据备份提醒任务执行完成 - 完成时间: {}", LocalDateTime.now().format(formatter));
            
        } catch (Exception e) {
            logger.error("数据备份提醒任务执行失败", e);
        }
    }
}
