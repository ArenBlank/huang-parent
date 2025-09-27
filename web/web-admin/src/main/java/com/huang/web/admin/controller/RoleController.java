package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.common.result.Result;
import com.huang.common.result.ResultCodeEnum;
import com.huang.model.entity.Role;
import com.huang.model.entity.UserRole;
import com.huang.web.admin.dto.role.RoleQueryDTO;
import com.huang.web.admin.dto.role.RoleStatusUpdateDTO;
import com.huang.web.admin.vo.role.RoleListVO;
import com.huang.web.admin.vo.role.RoleStatisticsVO;
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
 * 角色管理控制器
 * @author system
 * @since 2025-01-24
 */
@Tag(name = "角色管理", description = "角色管理相关接口")
@RestController
@RequestMapping("/admin/role")
@Validated
@Slf4j
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleService userRoleService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 角色列表管理
     */
    @Operation(summary = "角色列表查询", description = "支持分页和多条件筛选的角色列表查询")
    @GetMapping("/list")
    public Result<Page<RoleListVO>> getRoleList(@Valid RoleQueryDTO queryDTO) {
        try {
            // 构建分页对象
            Page<Role> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
            
            // 构建查询条件
            LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
            
            // 角色名称模糊查询
            if (StringUtils.hasText(queryDTO.getRoleName())) {
                queryWrapper.like(Role::getRoleName, queryDTO.getRoleName());
            }
            
            // 角色编码模糊查询
            if (StringUtils.hasText(queryDTO.getRoleCode())) {
                queryWrapper.like(Role::getRoleCode, queryDTO.getRoleCode());
            }
            
            // 状态筛选
            if (queryDTO.getStatus() != null) {
                queryWrapper.eq(Role::getStatus, queryDTO.getStatus());
            }
            
            // 时间范围筛选
            if (StringUtils.hasText(queryDTO.getStartTime())) {
                queryWrapper.ge(Role::getCreateTime, LocalDateTime.parse(queryDTO.getStartTime(), DATE_TIME_FORMATTER));
            }
            if (StringUtils.hasText(queryDTO.getEndTime())) {
                queryWrapper.le(Role::getCreateTime, LocalDateTime.parse(queryDTO.getEndTime(), DATE_TIME_FORMATTER));
            }
            
            // 排序
            if ("asc".equalsIgnoreCase(queryDTO.getSortOrder())) {
                queryWrapper.orderByAsc(Role::getCreateTime);
            } else {
                queryWrapper.orderByDesc(Role::getCreateTime);
            }
            
            // 执行查询
            Page<Role> rolePage = roleService.page(page, queryWrapper);
            
            // 转换为VO
            Page<RoleListVO> resultPage = new Page<>();
            BeanUtils.copyProperties(rolePage, resultPage);
            
            List<RoleListVO> roleListVOs = rolePage.getRecords().stream().map(role -> {
                RoleListVO vo = new RoleListVO();
                BeanUtils.copyProperties(role, vo);
                
                // 设置状态描述
                vo.setStatusDesc(getStatusDesc(role.getStatus()));
                
                // 查询角色用户数量
                vo.setUserCount(getRoleUserCount(role.getId()));
                
                return vo;
            }).collect(Collectors.toList());
            
            resultPage.setRecords(roleListVOs);
            
            return Result.ok(resultPage);
            
        } catch (Exception e) {
            log.error("查询角色列表失败", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "查询角色列表失败：" + e.getMessage());
        }
    }

    /**
     * 角色详情查看
     */
    @Operation(summary = "角色详情查看", description = "根据角色ID查看角色详细信息")
    @GetMapping("/detail/{roleId}")
    public Result<RoleListVO> getRoleDetail(
            @Parameter(description = "角色ID", required = true)
            @PathVariable @NotNull @Min(1) Long roleId) {
        try {
            Role role = roleService.getById(roleId);
            if (role == null) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "角色不存在");
            }
            
            RoleListVO detailVO = new RoleListVO();
            BeanUtils.copyProperties(role, detailVO);
            
            // 设置状态描述
            detailVO.setStatusDesc(getStatusDesc(role.getStatus()));
            
            // 查询角色用户数量
            detailVO.setUserCount(getRoleUserCount(roleId));
            
            return Result.ok(detailVO);
            
        } catch (Exception e) {
            log.error("查询角色详情失败，roleId: {}", roleId, e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "查询角色详情失败：" + e.getMessage());
        }
    }

    /**
     * 角色状态管理
     */
    @Operation(summary = "角色状态管理", description = "启用或禁用角色")
    @PutMapping("/status")
    public Result<Void> updateRoleStatus(@RequestBody @Valid RoleStatusUpdateDTO statusUpdateDTO) {
        try {
            Role role = roleService.getById(statusUpdateDTO.getRoleId());
            if (role == null) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "角色不存在");
            }
            
            // 检查是否有用户正在使用该角色（如果要禁用的话）
            if (statusUpdateDTO.getStatus() == 0) {
                long userCount = getRoleUserCount(statusUpdateDTO.getRoleId());
                if (userCount > 0) {
                    return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), 
                            String.format("该角色下还有 %d 个用户，无法禁用", userCount));
                }
            }
            
            // 更新角色状态
            LambdaUpdateWrapper<Role> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Role::getId, statusUpdateDTO.getRoleId())
                         .set(Role::getStatus, statusUpdateDTO.getStatus())
                         .set(Role::getUpdateTime, LocalDateTime.now());
            
            boolean success = roleService.update(updateWrapper);
            if (success) {
                String action = statusUpdateDTO.getStatus() == 1 ? "启用" : "禁用";
                log.info("{}角色成功，roleId: {}, 备注: {}", action, statusUpdateDTO.getRoleId(), statusUpdateDTO.getRemark());
                return Result.ok();
            } else {
                return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "更新角色状态失败");
            }
            
        } catch (Exception e) {
            log.error("更新角色状态失败", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "更新角色状态失败：" + e.getMessage());
        }
    }

    /**
     * 角色状态管理 - 表单参数版本
     */
    @Operation(summary = "角色状态管理（表单参数）", description = "使用表单参数启用或禁用角色，便于在API文档中测试")
    @PutMapping("/status-form")
    public Result<Void> updateRoleStatusForm(
            @Parameter(description = "角色ID", required = true)
            @RequestParam @NotNull @Min(1) Long roleId,
            @Parameter(description = "状态：0-禁用，1-正常", required = true)
            @RequestParam @NotNull Integer status,
            @Parameter(description = "备注")
            @RequestParam(required = false) String remark) {
        try {
            // 验证状态值
            if (status != 0 && status != 1) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "状态值只能是0（禁用）或1（正常）");
            }
            
            Role role = roleService.getById(roleId);
            if (role == null) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "角色不存在");
            }
            
            // 检查是否有用户正在使用该角色（如果要禁用的话）
            if (status == 0) {
                long userCount = getRoleUserCount(roleId);
                if (userCount > 0) {
                    return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), 
                            String.format("该角色下还有 %d 个用户，无法禁用", userCount));
                }
            }
            
            // 更新角色状态
            LambdaUpdateWrapper<Role> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Role::getId, roleId)
                         .set(Role::getStatus, status)
                         .set(Role::getUpdateTime, LocalDateTime.now());
            
            boolean success = roleService.update(updateWrapper);
            if (success) {
                String action = status == 1 ? "启用" : "禁用";
                log.info("{}角色成功，roleId: {}, 备注: {}", action, roleId, remark);
                return Result.ok();
            } else {
                return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "更新角色状态失败");
            }
            
        } catch (Exception e) {
            log.error("更新角色状态失败", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "更新角色状态失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有可用角色（用于下拉选择）
     */
    @Operation(summary = "获取可用角色列表", description = "获取所有启用状态的角色，用于下拉选择")
    @GetMapping("/available")
    public Result<List<RoleListVO>> getAvailableRoles() {
        try {
            LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Role::getStatus, 1)
                       .orderByAsc(Role::getRoleName);
            
            List<Role> roles = roleService.list(queryWrapper);
            
            List<RoleListVO> roleVOs = roles.stream().map(role -> {
                RoleListVO vo = new RoleListVO();
                BeanUtils.copyProperties(role, vo);
                vo.setStatusDesc(getStatusDesc(role.getStatus()));
                vo.setUserCount(getRoleUserCount(role.getId()));
                return vo;
            }).collect(Collectors.toList());
            
            return Result.ok(roleVOs);
            
        } catch (Exception e) {
            log.error("获取可用角色列表失败", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "获取可用角色列表失败：" + e.getMessage());
        }
    }

    /**
     * 角色用户统计
     */
    @Operation(summary = "角色数据统计", description = "获取角色相关的统计数据")
    @GetMapping("/statistics")
    public Result<RoleStatisticsVO> getRoleStatistics() {
        try {
            RoleStatisticsVO statisticsVO = new RoleStatisticsVO();
            
            // 总角色数
            statisticsVO.setTotalRoles(roleService.count());
            
            // 正常状态角色数
            LambdaQueryWrapper<Role> activeWrapper = new LambdaQueryWrapper<>();
            activeWrapper.eq(Role::getStatus, 1);
            statisticsVO.setActiveRoles(roleService.count(activeWrapper));
            
            // 禁用状态角色数
            LambdaQueryWrapper<Role> inactiveWrapper = new LambdaQueryWrapper<>();
            inactiveWrapper.eq(Role::getStatus, 0);
            statisticsVO.setInactiveRoles(roleService.count(inactiveWrapper));
            
            // 角色用户分布统计
            statisticsVO.setRoleUserStatistics(getRoleUserStatistics());
            
            // 角色状态分布统计
            statisticsVO.setRoleStatusStatistics(getRoleStatusStatistics());
            
            return Result.ok(statisticsVO);
            
        } catch (Exception e) {
            log.error("获取角色统计数据失败", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "获取角色统计数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取角色下的用户列表
     */
    @Operation(summary = "获取角色用户列表", description = "获取指定角色下的所有用户")
    @GetMapping("/{roleId}/users")
    public Result<Map<String, Object>> getRoleUsers(
            @Parameter(description = "角色ID", required = true)
            @PathVariable @NotNull @Min(1) Long roleId,
            @Parameter(description = "页码")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            // 验证角色是否存在
            Role role = roleService.getById(roleId);
            if (role == null) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "角色不存在");
            }
            
            // 查询角色下的用户关联
            LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserRole::getRoleId, roleId);
            
            Page<UserRole> userRolePage = new Page<>(page, size);
            userRolePage = userRoleService.page(userRolePage, wrapper);
            
            Map<String, Object> result = new HashMap<>();
            result.put("roleId", roleId);
            result.put("roleName", role.getRoleName());
            result.put("roleCode", role.getRoleCode());
            result.put("total", userRolePage.getTotal());
            result.put("pages", userRolePage.getPages());
            result.put("current", userRolePage.getCurrent());
            result.put("size", userRolePage.getSize());
            result.put("userRoles", userRolePage.getRecords());
            
            return Result.ok(result);
            
        } catch (Exception e) {
            log.error("获取角色用户列表失败，roleId: {}", roleId, e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "获取角色用户列表失败：" + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) return "未知";
        return status == 1 ? "正常" : "禁用";
    }

    /**
     * 获取角色用户数量
     */
    private Long getRoleUserCount(Long roleId) {
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getRoleId, roleId);
        return userRoleService.count(wrapper);
    }

    /**
     * 获取角色用户分布统计
     */
    private List<RoleStatisticsVO.RoleUserStatistic> getRoleUserStatistics() {
        List<Role> allRoles = roleService.list();
        long totalUserRoles = userRoleService.count();
        
        return allRoles.stream().map(role -> {
            long userCount = getRoleUserCount(role.getId());
            
            RoleStatisticsVO.RoleUserStatistic statistic = new RoleStatisticsVO.RoleUserStatistic();
            statistic.setRoleId(role.getId());
            statistic.setRoleName(role.getRoleName());
            statistic.setRoleCode(role.getRoleCode());
            statistic.setUserCount(userCount);
            statistic.setPercentage(totalUserRoles > 0 ? (double) userCount / totalUserRoles * 100 : 0.0);
            
            return statistic;
        }).collect(Collectors.toList());
    }

    /**
     * 获取角色状态分布统计
     */
    private List<RoleStatisticsVO.RoleStatusStatistic> getRoleStatusStatistics() {
        List<Role> allRoles = roleService.list();
        Map<Integer, Long> statusCountMap = allRoles.stream()
                .collect(Collectors.groupingBy(
                    role -> role.getStatus() == null ? 0 : role.getStatus(),
                    Collectors.counting()
                ));
        
        long totalCount = allRoles.size();
        
        return Arrays.asList(0, 1).stream().map(status -> {
            Long count = statusCountMap.getOrDefault(status, 0L);
            RoleStatisticsVO.RoleStatusStatistic statistic = new RoleStatisticsVO.RoleStatusStatistic();
            statistic.setStatus(status);
            statistic.setStatusDesc(getStatusDesc(status));
            statistic.setCount(count);
            statistic.setPercentage(totalCount > 0 ? (double) count / totalCount * 100 : 0.0);
            return statistic;
        }).collect(Collectors.toList());
    }
}