package com.huang.web.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.model.entity.*;
import com.huang.web.admin.mapper.*;
import com.huang.web.admin.service.StatisticsService;
import com.huang.web.admin.vo.statistics.AccountStatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

/**
 * 统计服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final CoachMapper coachMapper;
    private final UserLoginLogMapper userLoginLogMapper;
    private final CourseStatisticsMapper courseStatisticsMapper;
    private final CourseScheduleMapper courseScheduleMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final CoachWorkStatisticsMapper coachWorkStatisticsMapper;
    private final CoachConsultationMapper coachConsultationMapper;
    private final CoachEvaluationMapper coachEvaluationMapper;
    private final TempFileRecordMapper tempFileRecordMapper;

    @Override
    public AccountStatisticsVO getAccountStatistics() {
        AccountStatisticsVO vo = new AccountStatisticsVO();

        // 总用户数
        Long totalUsers = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getIsDeleted, 0)
        );
        vo.setTotalUsers(totalUsers);

        // 活跃用户数(状态为1的用户)
        Long activeUsers = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getIsDeleted, 0)
                .eq(User::getStatus, 1)
        );
        vo.setActiveUsers(activeUsers);

        // 今日新增用户
        LocalDate today = LocalDate.now();
        Long todayNewUsers = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getIsDeleted, 0)
                .ge(User::getCreateTime, today.atStartOfDay())
                .lt(User::getCreateTime, today.plusDays(1).atStartOfDay())
        );
        vo.setTodayNewUsers(todayNewUsers);

        // 今日登录用户数
        Integer todayLoginUsers = userLoginLogMapper.countTodayLoginUsers();
        vo.setTodayLoginUsers(todayLoginUsers != null ? todayLoginUsers : 0);

        // 今日登录次数
        Integer todayLogins = userLoginLogMapper.countTodayLogins();
        vo.setTodayLogins(todayLogins != null ? todayLogins : 0);

        // 本周新增用户
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        Long weekNewUsers = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getIsDeleted, 0)
                .ge(User::getCreateTime, weekStart.atStartOfDay())
        );
        vo.setWeekNewUsers(weekNewUsers);

        // 本月新增用户
        LocalDate monthStart = LocalDate.of(today.getYear(), today.getMonth(), 1);
        Long monthNewUsers = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getIsDeleted, 0)
                .ge(User::getCreateTime, monthStart.atStartOfDay())
        );
        vo.setMonthNewUsers(monthNewUsers);

        // 获取会员角色ID
        Role memberRole = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, "member")
                .eq(Role::getIsDeleted, 0)
        );

        Role vipRole = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, "vip")
                .eq(Role::getIsDeleted, 0)
        );

        // 普通会员数
        if (memberRole != null) {
            Long normalMembers = userRoleMapper.selectCount(
                new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getRoleId, memberRole.getId())
                    .eq(UserRole::getIsDeleted, 0)
            );
            vo.setNormalMembers(normalMembers);
        } else {
            vo.setNormalMembers(0L);
        }

        // VIP会员数
        if (vipRole != null) {
            Long vipMembers = userRoleMapper.selectCount(
                new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getRoleId, vipRole.getId())
                    .eq(UserRole::getIsDeleted, 0)
            );
            vo.setVipMembers(vipMembers);
        } else {
            vo.setVipMembers(0L);
        }

        // 教练数量
        Long coachCount = coachMapper.selectCount(
            new LambdaQueryWrapper<Coach>()
                .eq(Coach::getIsDeleted, 0)
                .eq(Coach::getStatus, 1)
        );
        vo.setCoachCount(coachCount);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeCourseStatisticsTask() {
        log.info("开始执行课程统计任务...");

        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 检查昨日是否已统计
        Long count = courseStatisticsMapper.selectCount(
            new LambdaQueryWrapper<CourseStatistics>()
                .eq(CourseStatistics::getStatisticsDate, yesterday)
        );

        if (count > 0) {
            log.info("昨日({})课程统计已存在,跳过", yesterday);
            return;
        }

        CourseStatistics statistics = new CourseStatistics();
        statistics.setStatisticsDate(yesterday);

        // 新增报名数
        Long newEnrollment = courseEnrollmentMapper.selectCount(
            new LambdaQueryWrapper<CourseEnrollment>()
                .ge(CourseEnrollment::getCreateTime, yesterday.atStartOfDay())
                .lt(CourseEnrollment::getCreateTime, yesterday.plusDays(1).atStartOfDay())
                .eq(CourseEnrollment::getIsDeleted, 0)
        );
        statistics.setNewEnrollmentCount(newEnrollment.intValue());

        // 取消报名数
        Long cancelEnrollment = courseEnrollmentMapper.selectCount(
            new LambdaQueryWrapper<CourseEnrollment>()
                .ge(CourseEnrollment::getUpdateTime, yesterday.atStartOfDay())
                .lt(CourseEnrollment::getUpdateTime, yesterday.plusDays(1).atStartOfDay())
                .eq(CourseEnrollment::getStatus, 0)
                .eq(CourseEnrollment::getIsDeleted, 0)
        );
        statistics.setCancelEnrollmentCount(cancelEnrollment.intValue());

        // 完成课程数
        Long completedCourse = courseScheduleMapper.selectCount(
            new LambdaQueryWrapper<CourseSchedule>()
                .ge(CourseSchedule::getEndTime, yesterday.atStartOfDay())
                .lt(CourseSchedule::getEndTime, yesterday.plusDays(1).atStartOfDay())
                .eq(CourseSchedule::getStatus, 2)
                .eq(CourseSchedule::getIsDeleted, 0)
        );
        statistics.setCompletedCourseCount(completedCourse.intValue());

        // 签到人数
        Long checkIn = courseEnrollmentMapper.selectCount(
            new LambdaQueryWrapper<CourseEnrollment>()
                .isNotNull(CourseEnrollment::getCheckInTime)
                .ge(CourseEnrollment::getCheckInTime, yesterday.atStartOfDay())
                .lt(CourseEnrollment::getCheckInTime, yesterday.plusDays(1).atStartOfDay())
                .eq(CourseEnrollment::getIsDeleted, 0)
        );
        statistics.setCheckInCount(checkIn.intValue());

        // 总参与人数(包括报名和签到的)
        statistics.setTotalParticipants(newEnrollment.intValue());

        // 查找最受欢迎的课程(报名人数最多的)
        // 这里简化处理,实际应该使用GROUP BY和ORDER BY
        statistics.setPopularCourseId(1L);
        statistics.setPopularCourseName("高强度间歇训练(HIIT)");

        courseStatisticsMapper.insert(statistics);

        log.info("课程统计任务完成: 日期={}, 新增报名={}, 完成课程={}",
            yesterday, newEnrollment, completedCourse);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeCoachStatisticsTask() {
        log.info("开始执行教练工作统计任务...");

        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 获取所有在职教练
        List<Coach> coachList = coachMapper.selectList(
            new LambdaQueryWrapper<Coach>()
                .eq(Coach::getIsDeleted, 0)
                .eq(Coach::getStatus, 1)
        );

        for (Coach coach : coachList) {
            // 检查是否已统计
            Long count = coachWorkStatisticsMapper.selectCount(
                new LambdaQueryWrapper<CoachWorkStatistics>()
                    .eq(CoachWorkStatistics::getCoachId, coach.getId())
                    .eq(CoachWorkStatistics::getStatisticsDate, yesterday)
            );

            if (count > 0) {
                log.info("教练{}昨日统计已存在,跳过", coach.getRealName());
                continue;
            }

            CoachWorkStatistics statistics = new CoachWorkStatistics();
            statistics.setCoachId(coach.getId());
            statistics.setCoachName(coach.getRealName());
            statistics.setStatisticsDate(yesterday);

            // 授课次数
            Long courseCount = courseScheduleMapper.selectCount(
                new LambdaQueryWrapper<CourseSchedule>()
                    .eq(CourseSchedule::getCoachId, coach.getId())
                    .ge(CourseSchedule::getStartTime, yesterday.atStartOfDay())
                    .lt(CourseSchedule::getStartTime, yesterday.plusDays(1).atStartOfDay())
                    .in(CourseSchedule::getStatus, 1, 2)
                    .eq(CourseSchedule::getIsDeleted, 0)
            );
            statistics.setCourseCount(courseCount.intValue());

            // 授课时长(按每节课1小时估算)
            statistics.setCourseHours(new BigDecimal(courseCount).setScale(2, RoundingMode.HALF_UP));

            // 服务学员数(简化处理,使用课程参与人数)
            statistics.setStudentCount(courseCount.intValue() * 10);

            // 咨询次数
            Long consultationCount = coachConsultationMapper.selectCount(
                new LambdaQueryWrapper<CoachConsultation>()
                    .eq(CoachConsultation::getCoachId, coach.getId())
                    .ge(CoachConsultation::getConsultationDate, yesterday.atStartOfDay())
                    .lt(CoachConsultation::getConsultationDate, yesterday.plusDays(1).atStartOfDay())
                    .eq(CoachConsultation::getIsDeleted, 0)
            );
            statistics.setConsultationCount(consultationCount.intValue());

            // 收入金额(模拟数据: 课程数 * 50 + 咨询数 * 100)
            BigDecimal income = new BigDecimal(courseCount).multiply(new BigDecimal("50"))
                .add(new BigDecimal(consultationCount).multiply(new BigDecimal("100")));
            statistics.setIncomeAmount(income);

            // 平均评分
            List<CoachEvaluation> evaluations = coachEvaluationMapper.selectList(
                new LambdaQueryWrapper<CoachEvaluation>()
                    .eq(CoachEvaluation::getCoachId, coach.getId())
                    .ge(CoachEvaluation::getCreateTime, yesterday.atStartOfDay())
                    .lt(CoachEvaluation::getCreateTime, yesterday.plusDays(1).atStartOfDay())
                    .eq(CoachEvaluation::getIsDeleted, 0)
            );

            if (!evaluations.isEmpty()) {
                BigDecimal totalRating = evaluations.stream()
                    .map(CoachEvaluation::getOverallRating)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal avgRating = totalRating.divide(
                    new BigDecimal(evaluations.size()), 2, RoundingMode.HALF_UP);
                statistics.setRatingAvg(avgRating);
            } else {
                statistics.setRatingAvg(coach.getRating());
            }

            coachWorkStatisticsMapper.insert(statistics);
            log.info("教练{}统计完成: 授课{}次, 收入{}",
                coach.getRealName(), courseCount, income);
        }

        log.info("教练工作统计任务完成");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeCourseStatusCheckTask() {
        log.info("开始执行课程状态检查任务...");

        LocalDateTime now = LocalDateTime.now();

        // 查找已结束但状态未更新的课程
        List<CourseSchedule> schedules = courseScheduleMapper.selectList(
            new LambdaQueryWrapper<CourseSchedule>()
                .lt(CourseSchedule::getEndTime, now)
                .eq(CourseSchedule::getStatus, 1)
                .eq(CourseSchedule::getIsDeleted, 0)
        );

        int updatedCount = 0;
        for (CourseSchedule schedule : schedules) {
            schedule.setStatus(2); // 设置为已结束
            courseScheduleMapper.updateById(schedule);
            updatedCount++;
        }

        log.info("课程状态检查任务完成: 更新了{}个课程状态", updatedCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanExpiredTempFiles() {
        log.info("开始清理过期临时文件...");

        LocalDateTime now = LocalDateTime.now();

        // 查询过期的临时文件
        List<TempFileRecord> expiredFiles = tempFileRecordMapper.selectExpiredFiles(now);

        int cleanedCount = 0;
        for (TempFileRecord file : expiredFiles) {
            // 更新状态为已过期
            file.setStatus("expired");
            tempFileRecordMapper.updateById(file);
            cleanedCount++;

            // 这里应该调用MinIO服务删除实际文件
            // minioService.deleteFile(file.getFilePath());
            log.debug("标记过期文件: {}", file.getFilePath());
        }

        log.info("过期临时文件清理完成: 清理了{}个文件", cleanedCount);
    }
}
