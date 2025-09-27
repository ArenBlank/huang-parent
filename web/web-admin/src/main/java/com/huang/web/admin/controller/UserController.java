package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.common.result.Result;
import com.huang.common.result.ResultCodeEnum;
import com.huang.model.entity.User;
import com.huang.model.entity.Role;
import com.huang.model.entity.UserRole;
import com.huang.web.admin.dto.user.UserQueryDTO;
import com.huang.web.admin.dto.user.UserStatusUpdateDTO;
import com.huang.web.admin.dto.user.UserRoleAssignDTO;
import com.huang.web.admin.vo.user.UserListVO;
import com.huang.web.admin.vo.user.UserDetailVO;
import com.huang.web.admin.vo.user.UserStatisticsVO;
import com.huang.web.admin.service.UserService;
import com.huang.web.admin.service.RoleService;
import com.huang.web.admin.service.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户管理控制器
 * @author system
 * @since 2025-01-24
 */
@Tag(name = "用户管理", description = "用户管理相关接口")
@RestController
@RequestMapping("/admin/user")
@Validated
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleService userRoleService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 用户列表查询（分页、筛选）
     */
    @Operation(summary = "用户列表查询", description = "支持分页和多条件筛选的用户列表查询")
    @GetMapping("/list")
    public Result<Page<UserListVO>> getUserList(@Valid UserQueryDTO queryDTO) {
        try {
            // 构建分页对象
            Page<User> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
            
            // 构建查询条件
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
            
            // 执行查询
            Page<User> userPage = userService.page(page, queryWrapper);
            
            // 转换为VO
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
            
            return Result.ok(resultPage);
            
        } catch (Exception e) {
            log.error("查询用户列表失败", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "查询用户列表失败：" + e.getMessage());
        }
    }

    /**
     * 用户详情查看
     */
    @Operation(summary = "用户详情查看", description = "根据用户ID查看用户详细信息")
    @GetMapping("/detail/{userId}")
    public Result<UserDetailVO> getUserDetail(
            @Parameter(description = "用户ID", required = true)
            @PathVariable @NotNull @Min(1) Long userId) {
        try {
            User user = userService.getById(userId);
            if (user == null) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "用户不存在");
            }
            
            UserDetailVO detailVO = new UserDetailVO();
            BeanUtils.copyProperties(user, detailVO);
            
            // 设置性别描述
            detailVO.setGenderDesc(getGenderDesc(user.getGender()));
            
            // 设置状态描述
            detailVO.setStatusDesc(getStatusDesc(user.getStatus()));
            
            // 查询用户角色详情
            detailVO.setRoles(getUserRoleDetails(userId));
            
            // TODO: 设置最后登录时间和登录次数（需要登录日志表支持）
            detailVO.setLastLoginTime(null);
            detailVO.setLoginCount(0);
            
            return Result.ok(detailVO);
            
        } catch (Exception e) {
            log.error("查询用户详情失败，userId: {}", userId, e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "查询用户详情失败：" + e.getMessage());
        }
    }

    /**
     * 用户状态管理（启用/禁用）
     */
    @Operation(summary = "用户状态管理", description = "启用或禁用用户")
    @PutMapping("/status")
    public Result<Void> updateUserStatus(@RequestBody @Valid UserStatusUpdateDTO statusUpdateDTO) {
        try {
            User user = userService.getById(statusUpdateDTO.getUserId());
            if (user == null) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "用户不存在");
            }
            
            // 更新用户状态
            LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(User::getId, statusUpdateDTO.getUserId())
                         .set(User::getStatus, statusUpdateDTO.getStatus())
                         .set(User::getUpdateTime, new Date());
            
            boolean success = userService.update(updateWrapper);
            if (success) {
                String action = statusUpdateDTO.getStatus() == 1 ? "启用" : "禁用";
                log.info("{}用户成功，userId: {}, 备注: {}", action, statusUpdateDTO.getUserId(), statusUpdateDTO.getRemark());
                return Result.ok();
            } else {
                return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "更新用户状态失败");
            }
            
        } catch (Exception e) {
            log.error("更新用户状态失败", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "更新用户状态失败：" + e.getMessage());
        }
    }

    /**
     * 用户状态管理（表单参数版本）
     */
    @Operation(summary = "用户状态管理（表单版）", description = "启用或禁用用户（使用表单参数，方便接口文档测试）")
    @PutMapping("/status-form")
    public Result<Void> updateUserStatusForm(
            @Parameter(description = "用户ID", required = true)
            @RequestParam @NotNull @Min(1) Long userId,
            @Parameter(description = "状态：0-禁用，1-启用", required = true)
            @RequestParam @NotNull Integer status,
            @Parameter(description = "备注")
            @RequestParam(required = false) String remark) {
        try {
            // 验证状态值
            if (status != 0 && status != 1) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "状态值只能为0（禁用）或1（启用）");
            }
            
            User user = userService.getById(userId);
            if (user == null) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "用户不存在");
            }
            
            // 更新用户状态
            LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(User::getId, userId)
                         .set(User::getStatus, status)
                         .set(User::getUpdateTime, new Date());
            
            boolean success = userService.update(updateWrapper);
            if (success) {
                String action = status == 1 ? "启用" : "禁用";
                log.info("{}\u7528户成功（表单版），userId: {}, 备注: {}", action, userId, remark);
                return Result.ok();
            } else {
                return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "更新用户状态失败");
            }
            
        } catch (Exception e) {
            log.error("更新用户状态失败（表单版）", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "更新用户状态失败：" + e.getMessage());
        }
    }

    /**
     * 用户角色分配
     */
    @Operation(summary = "用户角色分配", description = "为用户分配角色")
    @PostMapping("/assign-roles")
    public Result<Void> assignUserRoles(@RequestBody @Valid UserRoleAssignDTO assignDTO) {
        try {
            User user = userService.getById(assignDTO.getUserId());
            if (user == null) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "用户不存在");
            }
            
            // 验证角色是否存在
            List<Role> roles = roleService.listByIds(assignDTO.getRoleIds());
            if (roles.size() != assignDTO.getRoleIds().size()) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "部分角色不存在");
            }
            
            // 删除用户现有角色
            LambdaQueryWrapper<UserRole> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(UserRole::getUserId, assignDTO.getUserId());
            userRoleService.remove(deleteWrapper);
            
            // 添加新角色
            List<UserRole> userRoles = assignDTO.getRoleIds().stream().map(roleId -> {
                UserRole userRole = new UserRole();
                userRole.setUserId(assignDTO.getUserId());
                userRole.setRoleId(roleId);
                userRole.setCreateTime(new Date());
                return userRole;
            }).collect(Collectors.toList());
            
            boolean success = userRoleService.saveBatch(userRoles);
            if (success) {
                log.info("用户角色分配成功，userId: {}, roleIds: {}, 备注: {}", 
                        assignDTO.getUserId(), assignDTO.getRoleIds(), assignDTO.getRemark());
                return Result.ok();
            } else {
                return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "用户角色分配失败");
            }
            
        } catch (Exception e) {
            log.error("用户角色分配失败", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "用户角色分配失败：" + e.getMessage());
        }
    }

    /**
     * 用户角色分配（表单参数版本）
     */
    @Operation(summary = "用户角色分配（表单版）", description = "为用户分配角色（使用表单参数，方便接口文档测试）")
    @PostMapping("/assign-roles-form")
    public Result<Void> assignUserRolesForm(
            @Parameter(description = "用户ID", required = true)
            @RequestParam @NotNull @Min(1) Long userId,
            @Parameter(description = "角色ID列表（用逗号分隔）", required = true)
            @RequestParam @NotNull String roleIds,
            @Parameter(description = "备注")
            @RequestParam(required = false) String remark) {
        try {
            // 验证用户是否存在
            User user = userService.getById(userId);
            if (user == null) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "用户不存在");
            }
            
            // 解析角色ID列表
            List<Long> roleIdList;
            try {
                roleIdList = Arrays.stream(roleIds.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::valueOf)
                        .collect(Collectors.toList());
            } catch (NumberFormatException e) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "角色ID格式不正确，请使用逗号分隔的数字");
            }
            
            if (roleIdList.isEmpty()) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "角色ID列表不能为空");
            }
            
            // 验证角色是否存在
            List<Role> roles = roleService.listByIds(roleIdList);
            if (roles.size() != roleIdList.size()) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "部分角色不存在");
            }
            
            // 删除用户现有角色
            LambdaQueryWrapper<UserRole> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(UserRole::getUserId, userId);
            userRoleService.remove(deleteWrapper);
            
            // 添加新角色
            List<UserRole> userRoles = roleIdList.stream().map(roleId -> {
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRole.setCreateTime(new Date());
                return userRole;
            }).collect(Collectors.toList());
            
            boolean success = userRoleService.saveBatch(userRoles);
            if (success) {
                log.info("用户角色分配成功（表单版），userId: {}, roleIds: {}, 备注: {}", 
                        userId, roleIds, remark);
                return Result.ok();
            } else {
                return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "用户角色分配失败");
            }
            
        } catch (Exception e) {
            log.error("用户角色分配失败（表单版）", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "用户角色分配失败：" + e.getMessage());
        }
    }

    /**
     * 用户数据统计
     */
    @Operation(summary = "用户数据统计", description = "获取用户相关的统计数据")
    @GetMapping("/statistics")
    public Result<UserStatisticsVO> getUserStatistics() {
        try {
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
            
            // 最近7天用户登录趋势（暂时返回空数据，需要登录日志表支持）
            statisticsVO.setDailyLoginTrend(new ArrayList<>());
            
            return Result.ok(statisticsVO);
            
        } catch (Exception e) {
            log.error("获取用户统计数据失败", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "获取用户统计数据失败：" + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

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
        Map<Long, Date> userRoleMap = userRoles.stream()
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
            Date assignDate = userRoleMap.get(role.getId());
            roleDetail.setAssignTime(assignDate != null ? assignDate.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime() : null);
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
}