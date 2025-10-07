package com.huang.web.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.model.entity.Role;
import com.huang.model.entity.User;
import com.huang.model.entity.UserRole;
import com.huang.web.admin.dto.user.UserQueryDTO;
import com.huang.web.admin.dto.user.UserStatusUpdateDTO;
import com.huang.web.admin.dto.user.UserRoleAssignDTO;
import com.huang.web.admin.service.AdminUserService;
import com.huang.web.admin.service.UserService;
import com.huang.web.admin.service.RoleService;
import com.huang.web.admin.service.UserRoleService;
import com.huang.web.admin.vo.user.UserDetailVO;
import com.huang.web.admin.vo.user.UserListVO;
import com.huang.web.admin.vo.user.UserStatisticsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin端用户业务服务实现类
 * @author system
 * @since 2025-01-24
 */
@Slf4j
@Service
public class AdminUserServiceImpl implements AdminUserService {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleService userRoleService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Page<UserListVO> getUserList(UserQueryDTO queryDTO) {
        // 构建分页对象
        Page<User> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        
        // 构建查询条件
        LambdaQueryWrapper<User> queryWrapper = buildUserQueryWrapper(queryDTO);
        
        // 执行查询
        Page<User> userPage = userService.page(page, queryWrapper);
        
        // 转换为VO
        return convertToUserListVOPage(userPage);
    }

    @Override
    public UserDetailVO getUserDetail(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return null;
        }
        
        UserDetailVO detailVO = new UserDetailVO();
        BeanUtils.copyProperties(user, detailVO);
        
        // 设置性别描述
        detailVO.setGenderDesc(getGenderDesc(user.getGender()));
        
        // 设置状态描述
        detailVO.setStatusDesc(getStatusDesc(user.getStatus()));
        
        // 查询用户角色详情
        detailVO.setRoles(getUserRoleDetails(userId));
        
        // 设置最后登录时间和登录次数
        // detailVO.setLastLoginTime(user.getLastLoginTime());
        // detailVO.setLoginCount(calculateLoginCount(userId));
        
        return detailVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserStatus(UserStatusUpdateDTO statusUpdateDTO) {
        User user = userService.getById(statusUpdateDTO.getUserId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 更新用户状态
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, statusUpdateDTO.getUserId())
                     .set(User::getStatus, statusUpdateDTO.getStatus())
                     .set(User::getUpdateTime, LocalDateTime.now());
        
        boolean result = userService.update(updateWrapper);
        
        if (result) {
            String action = statusUpdateDTO.getStatus() == 1 ? "启用" : "禁用";
            log.info("{}用户成功，userId: {}, 备注: {}", action, statusUpdateDTO.getUserId(), statusUpdateDTO.getRemark());
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignUserRoles(UserRoleAssignDTO assignDTO) {
        User user = userService.getById(assignDTO.getUserId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 验证角色是否存在
        List<Role> roles = roleService.listByIds(assignDTO.getRoleIds());
        if (roles.size() != assignDTO.getRoleIds().size()) {
            throw new RuntimeException("部分角色不存在");
        }
        
        // 使用逻辑删除删除用户现有角色（数据库约束已修复以支持逻辑删除）
        LambdaQueryWrapper<UserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(UserRole::getUserId, assignDTO.getUserId());
        boolean deleteResult = userRoleService.remove(deleteWrapper);
        
        if (deleteResult) {
            log.info("逻辑删除用户现有角色关系成功，userId: {}", assignDTO.getUserId());
        }
        
        // 添加新角色
        List<UserRole> userRoles = assignDTO.getRoleIds().stream().map(roleId -> {
            UserRole userRole = new UserRole();
            userRole.setUserId(assignDTO.getUserId());
            userRole.setRoleId(roleId);
            userRole.setCreateTime(LocalDateTime.now());
            return userRole;
        }).collect(Collectors.toList());
        
        boolean result = userRoleService.saveBatch(userRoles);
        
        if (result) {
            log.info("用户角色分配成功，userId: {}, roleIds: {}, 备注: {}", 
                    assignDTO.getUserId(), assignDTO.getRoleIds(), assignDTO.getRemark());
        }
        
        return result;
    }

    @Override
    public UserStatisticsVO getUserStatistics() {
        UserStatisticsVO statisticsVO = new UserStatisticsVO();
        
        // 总用户数
        statisticsVO.setTotalUsers(userService.count());
        
        // 正常状态用户数
        LambdaQueryWrapper<User> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(User::getStatus, 1);
        statisticsVO.setActiveUsers(userService.count(activeWrapper));
        
        // 禁用状态用户数
        LambdaQueryWrapper<User> inactiveWrapper = new LambdaQueryWrapper<>();
        inactiveWrapper.eq(User::getStatus, 0);
        statisticsVO.setInactiveUsers(userService.count(inactiveWrapper));
        
        // 今日新增用户数
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LambdaQueryWrapper<User> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(User::getCreateTime, todayStart);
        statisticsVO.setTodayNewUsers(userService.count(todayWrapper));
        
        // 本周新增用户数
        LocalDateTime weekStart = todayStart.minusDays(6);
        LambdaQueryWrapper<User> weekWrapper = new LambdaQueryWrapper<>();
        weekWrapper.ge(User::getCreateTime, weekStart);
        statisticsVO.setWeekNewUsers(userService.count(weekWrapper));
        
        // 本月新增用户数
        LocalDateTime monthStart = todayStart.withDayOfMonth(1);
        LambdaQueryWrapper<User> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.ge(User::getCreateTime, monthStart);
        statisticsVO.setMonthNewUsers(userService.count(monthWrapper));
        
        // 性别分布统计
        statisticsVO.setGenderStatistics(getGenderStatistics());
        
        // 角色分布统计
        statisticsVO.setRoleStatistics(getRoleStatistics());
        
        // 最近7天用户注册趋势
        statisticsVO.setDailyRegistrationTrend(getDailyRegistrationTrend());
        
        // 最近7天用户登录趋势
        statisticsVO.setDailyLoginTrend(new ArrayList<>());
        
        return statisticsVO;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建用户查询条件
     */
    private LambdaQueryWrapper<User> buildUserQueryWrapper(UserQueryDTO queryDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        
        // 用户名模糊查询
        if (StringUtils.hasText(queryDTO.getUsername())) {
            queryWrapper.like(User::getUsername, queryDTO.getUsername());
        }
        
        // 昵称模糊查询
        if (StringUtils.hasText(queryDTO.getNickname())) {
            queryWrapper.like(User::getNickname, queryDTO.getNickname());
        }
        
        // 邮箱精确查询
        if (StringUtils.hasText(queryDTO.getEmail())) {
            queryWrapper.eq(User::getEmail, queryDTO.getEmail());
        }
        
        // 手机号精确查询
        if (StringUtils.hasText(queryDTO.getPhone())) {
            queryWrapper.eq(User::getPhone, queryDTO.getPhone());
        }
        
        // 性别筛选
        if (queryDTO.getGender() != null) {
            queryWrapper.eq(User::getGender, queryDTO.getGender());
        }
        
        // 状态筛选
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq(User::getStatus, queryDTO.getStatus());
        }
        
        // 时间范围筛选
        if (StringUtils.hasText(queryDTO.getStartTime())) {
            queryWrapper.ge(User::getCreateTime, LocalDateTime.parse(queryDTO.getStartTime(), DATE_TIME_FORMATTER));
        }
        if (StringUtils.hasText(queryDTO.getEndTime())) {
            queryWrapper.le(User::getCreateTime, LocalDateTime.parse(queryDTO.getEndTime(), DATE_TIME_FORMATTER));
        }
        
        // 排序
        if ("asc".equalsIgnoreCase(queryDTO.getSortOrder())) {
            queryWrapper.orderByAsc(User::getCreateTime);
        } else {
            queryWrapper.orderByDesc(User::getCreateTime);
        }
        
        return queryWrapper;
    }

    /**
     * 转换为用户列表VO分页对象
     */
    private Page<UserListVO> convertToUserListVOPage(Page<User> userPage) {
        Page<UserListVO> resultPage = new Page<>();
        BeanUtils.copyProperties(userPage, resultPage);
        
        List<UserListVO> userListVOs = userPage.getRecords().stream().map(user -> {
            UserListVO vo = new UserListVO();
            BeanUtils.copyProperties(user, vo);
            
            // 设置性别描述
            vo.setGenderDesc(getGenderDesc(user.getGender()));
            
            // 设置状态描述
            vo.setStatusDesc(getStatusDesc(user.getStatus()));
            
            // 查询用户角色
            vo.setRoles(getUserRoles(user.getId()));
            
            return vo;
        }).collect(Collectors.toList());
        
        resultPage.setRecords(userListVOs);
        return resultPage;
    }

    /**
     * 获取性别描述
     */
    private String getGenderDesc(Integer gender) {
        if (gender == null) return "未知";
        switch (gender) {
            case 1: return "男";
            case 2: return "女";
            default: return "未知";
        }
    }

    /**
     * 获取状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) return "未知";
        return status == 1 ? "正常" : "禁用";
    }

    /**
     * 获取用户角色信息
     */
    private List<UserListVO.UserRoleInfo> getUserRoles(Long userId) {
        // 查询用户角色关联
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, userId);
        List<UserRole> userRoles = userRoleService.list(wrapper);
        
        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 查询角色详情
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        List<Role> roles = roleService.listByIds(roleIds);
        
        return roles.stream().map(role -> {
            UserListVO.UserRoleInfo roleInfo = new UserListVO.UserRoleInfo();
            roleInfo.setRoleId(role.getId());
            roleInfo.setRoleName(role.getRoleName());
            roleInfo.setRoleCode(role.getRoleCode());
            return roleInfo;
        }).collect(Collectors.toList());
    }

    /**
     * 获取用户角色详情
     */
    private List<UserDetailVO.UserRoleDetail> getUserRoleDetails(Long userId) {
        // 查询用户角色关联
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, userId);
        List<UserRole> userRoles = userRoleService.list(wrapper);
        
        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 创建用户角色映射
        Map<Long, LocalDateTime> userRoleMap = userRoles.stream()
                .collect(Collectors.toMap(UserRole::getRoleId, UserRole::getCreateTime));
        
        // 查询角色详情
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        List<Role> roles = roleService.listByIds(roleIds);
        
        return roles.stream().map(role -> {
            UserDetailVO.UserRoleDetail roleDetail = new UserDetailVO.UserRoleDetail();
            roleDetail.setRoleId(role.getId());
            roleDetail.setRoleName(role.getRoleName());
            roleDetail.setRoleCode(role.getRoleCode());
            roleDetail.setRoleDescription(role.getDescription());
            LocalDateTime assignDate = userRoleMap.get(role.getId());
            roleDetail.setAssignTime(assignDate);
            return roleDetail;
        }).collect(Collectors.toList());
    }

    /**
     * 获取性别分布统计
     */
    private List<UserStatisticsVO.GenderStatistic> getGenderStatistics() {
        List<User> allUsers = userService.list();
        Map<Integer, Long> genderCountMap = allUsers.stream()
                .collect(Collectors.groupingBy(
                    user -> user.getGender() == null ? 0 : user.getGender(),
                    Collectors.counting()
                ));
        
        long totalCount = allUsers.size();
        
        return Arrays.asList(0, 1, 2).stream().map(gender -> {
            Long count = genderCountMap.getOrDefault(gender, 0L);
            UserStatisticsVO.GenderStatistic statistic = new UserStatisticsVO.GenderStatistic();
            statistic.setGender(gender);
            statistic.setGenderDesc(getGenderDesc(gender));
            statistic.setCount(count);
            statistic.setPercentage(totalCount > 0 ? (double) count / totalCount * 100 : 0.0);
            return statistic;
        }).collect(Collectors.toList());
    }

    /**
     * 获取角色分布统计
     */
    private List<UserStatisticsVO.RoleStatistic> getRoleStatistics() {
        // 获取所有角色
        List<Role> allRoles = roleService.list();
        
        // 统计每个角色的用户数
        return allRoles.stream().map(role -> {
            LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserRole::getRoleId, role.getId());
            long userCount = userRoleService.count(wrapper);
            
            UserStatisticsVO.RoleStatistic statistic = new UserStatisticsVO.RoleStatistic();
            statistic.setRoleId(role.getId());
            statistic.setRoleName(role.getRoleName());
            statistic.setRoleCode(role.getRoleCode());
            statistic.setUserCount(userCount);
            
            long totalUsers = userService.count();
            statistic.setPercentage(totalUsers > 0 ? (double) userCount / totalUsers * 100 : 0.0);
            
            return statistic;
        }).collect(Collectors.toList());
    }

    /**
     * 获取最近7天用户注册趋势
     */
    private List<UserStatisticsVO.DailyStatistic> getDailyRegistrationTrend() {
        List<UserStatisticsVO.DailyStatistic> trend = new ArrayList<>();
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        
        for (int i = 6; i >= 0; i--) {
            LocalDateTime date = today.minusDays(i);
            LocalDateTime nextDate = date.plusDays(1);
            
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(User::getCreateTime, date)
                   .lt(User::getCreateTime, nextDate);
            
            long count = userService.count(wrapper);
            
            UserStatisticsVO.DailyStatistic statistic = new UserStatisticsVO.DailyStatistic();
            statistic.setDate(date.format(DateTimeFormatter.ofPattern("MM-dd")));
            statistic.setCount(count);
            trend.add(statistic);
        }
        
        return trend;
    }

    /**
     * 计算用户登录次数（模拟实现）
     */
    private int calculateLoginCount(Long userId) {
        // 模拟数据：根据用户ID生成一个随机登录次数
        return (int) (userId % 100) + 20; // 返20-119之间的模拟值
    }
}