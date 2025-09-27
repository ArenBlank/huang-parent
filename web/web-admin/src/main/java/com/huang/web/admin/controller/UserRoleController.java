package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.common.result.Result;
import com.huang.common.result.ResultCodeEnum;
import com.huang.model.entity.User;
import com.huang.model.entity.Role;
import com.huang.model.entity.UserRole;
import com.huang.web.admin.dto.userrole.BatchRoleAssignDTO;
import com.huang.web.admin.service.UserService;
import com.huang.web.admin.service.RoleService;
import com.huang.web.admin.service.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户角色关联管理控制器
 * @author system
 * @since 2025-01-24
 */
@Tag(name = "用户角色关联管理", description = "用户角色关联管理相关接口")
@RestController
@RequestMapping("/admin/user-role")
@Validated
@Slf4j
public class UserRoleController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleService userRoleService;

    /**
     * 获取用户角色关联列表
     */
    @Operation(summary = "用户角色关联列表", description = "获取所有用户角色关联信息")
    @GetMapping("/list")
    public Result<Page<Map<String, Object>>> getUserRoleList(
            @Parameter(description = "页码")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "用户ID")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "角色ID")
            @RequestParam(required = false) Long roleId) {
        try {
            Page<UserRole> userRolePage = new Page<>(page, size);
            LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
            
            if (userId != null) {
                wrapper.eq(UserRole::getUserId, userId);
            }
            if (roleId != null) {
                wrapper.eq(UserRole::getRoleId, roleId);
            }
            
            wrapper.orderByDesc(UserRole::getCreateTime);
            userRolePage = userRoleService.page(userRolePage, wrapper);
            
            // 构建返回数据
            List<Map<String, Object>> records = new ArrayList<>();
            if (!userRolePage.getRecords().isEmpty()) {
                // 获取用户信息
                List<Long> userIds = userRolePage.getRecords().stream()
                        .map(UserRole::getUserId)
                        .distinct()
                        .collect(Collectors.toList());
                Map<Long, User> userMap = userService.listByIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, user -> user));
                
                // 获取角色信息
                List<Long> roleIds = userRolePage.getRecords().stream()
                        .map(UserRole::getRoleId)
                        .distinct()
                        .collect(Collectors.toList());
                Map<Long, Role> roleMap = roleService.listByIds(roleIds).stream()
                        .collect(Collectors.toMap(Role::getId, role -> role));
                
                // 构建结果
                records = userRolePage.getRecords().stream().map(userRole -> {
                    Map<String, Object> record = new HashMap<>();
                    record.put("id", userRole.getId());
                    record.put("userId", userRole.getUserId());
                    record.put("roleId", userRole.getRoleId());
                    record.put("createTime", userRole.getCreateTime());
                    
                    // 用户信息
                    User user = userMap.get(userRole.getUserId());
                    if (user != null) {
                        record.put("username", user.getUsername());
                        record.put("nickname", user.getNickname());
                        record.put("userStatus", user.getStatus());
                        record.put("userStatusDesc", user.getStatus() == 1 ? "正常" : "禁用");
                    }
                    
                    // 角色信息
                    Role role = roleMap.get(userRole.getRoleId());
                    if (role != null) {
                        record.put("roleName", role.getRoleName());
                        record.put("roleCode", role.getRoleCode());
                        record.put("roleStatus", role.getStatus());
                        record.put("roleStatusDesc", role.getStatus() == 1 ? "正常" : "禁用");
                    }
                    
                    return record;
                }).collect(Collectors.toList());
            }
            
            Page<Map<String, Object>> resultPage = new Page<>(page, size);
            resultPage.setRecords(records);
            resultPage.setTotal(userRolePage.getTotal());
            resultPage.setPages(userRolePage.getPages());
            resultPage.setCurrent(userRolePage.getCurrent());
            resultPage.setSize(userRolePage.getSize());
            
            return Result.ok(resultPage);
            
        } catch (Exception e) {
            log.error("获取用户角色关联列表失败", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "获取用户角色关联列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取指定用户的角色列表
     */
    @Operation(summary = "获取用户角色", description = "获取指定用户的所有角色")
    @GetMapping("/user/{userId}/roles")
    public Result<List<Map<String, Object>>> getUserRoles(
            @Parameter(description = "用户ID", required = true)
            @PathVariable @NotNull @Min(1) Long userId) {
        try {
            // 验证用户是否存在
            User user = userService.getById(userId);
            if (user == null) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "用户不存在");
            }
            
            // 查询用户角色关联
            LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserRole::getUserId, userId);
            List<UserRole> userRoles = userRoleService.list(wrapper);
            
            if (userRoles.isEmpty()) {
                return Result.ok(new ArrayList<>());
            }
            
            // 查询角色详情
            List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
            List<Role> roles = roleService.listByIds(roleIds);
            
            // 构建结果
            Map<Long, Date> assignTimeMap = userRoles.stream()
                    .collect(Collectors.toMap(UserRole::getRoleId, userRole -> userRole.getCreateTime()));
            
            List<Map<String, Object>> result = roles.stream().map(role -> {
                Map<String, Object> roleInfo = new HashMap<>();
                roleInfo.put("roleId", role.getId());
                roleInfo.put("roleName", role.getRoleName());
                roleInfo.put("roleCode", role.getRoleCode());
                roleInfo.put("roleDescription", role.getDescription());
                roleInfo.put("roleStatus", role.getStatus());
                roleInfo.put("roleStatusDesc", role.getStatus() == 1 ? "正常" : "禁用");
                roleInfo.put("assignTime", assignTimeMap.get(role.getId()));
                return roleInfo;
            }).collect(Collectors.toList());
            
            return Result.ok(result);
            
        } catch (Exception e) {
            log.error("获取用户角色失败，userId: {}", userId, e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "获取用户角色失败：" + e.getMessage());
        }
    }

    /**
     * 获取指定角色的用户列表
     */
    @Operation(summary = "获取角色用户", description = "获取指定角色的所有用户")
    @GetMapping("/role/{roleId}/users")
    public Result<List<Map<String, Object>>> getRoleUsers(
            @Parameter(description = "角色ID", required = true)
            @PathVariable @NotNull @Min(1) Long roleId) {
        try {
            // 验证角色是否存在
            Role role = roleService.getById(roleId);
            if (role == null) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "角色不存在");
            }
            
            // 查询角色用户关联
            LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserRole::getRoleId, roleId);
            List<UserRole> userRoles = userRoleService.list(wrapper);
            
            if (userRoles.isEmpty()) {
                return Result.ok(new ArrayList<>());
            }
            
            // 查询用户详情
            List<Long> userIds = userRoles.stream().map(UserRole::getUserId).collect(Collectors.toList());
            List<User> users = userService.listByIds(userIds);
            
            // 构建结果
            Map<Long, Date> assignTimeMap = userRoles.stream()
                    .collect(Collectors.toMap(UserRole::getUserId, UserRole::getCreateTime));
            
            List<Map<String, Object>> result = users.stream().map(user -> {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("userId", user.getId());
                userInfo.put("username", user.getUsername());
                userInfo.put("nickname", user.getNickname());
                userInfo.put("email", user.getEmail());
                userInfo.put("phone", user.getPhone());
                userInfo.put("userStatus", user.getStatus());
                userInfo.put("userStatusDesc", user.getStatus() == 1 ? "正常" : "禁用");
                userInfo.put("assignTime", assignTimeMap.get(user.getId()));
                return userInfo;
            }).collect(Collectors.toList());
            
            return Result.ok(result);
            
        } catch (Exception e) {
            log.error("获取角色用户失败，roleId: {}", roleId, e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "获取角色用户失败：" + e.getMessage());
        }
    }

    /**
     * 批量角色分配
     */
    @Operation(summary = "批量角色分配", description = "为多个用户批量分配角色")
    @PostMapping("/batch-assign")
    public Result<Void> batchAssignRoles(@RequestBody @Valid BatchRoleAssignDTO batchAssignDTO) {
        try {
            // 验证用户是否存在
            List<User> users = userService.listByIds(batchAssignDTO.getUserIds());
            if (users.size() != batchAssignDTO.getUserIds().size()) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "部分用户不存在");
            }
            
            // 验证角色是否存在
            List<Role> roles = roleService.listByIds(batchAssignDTO.getRoleIds());
            if (roles.size() != batchAssignDTO.getRoleIds().size()) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "部分角色不存在");
            }
            
            String operation = batchAssignDTO.getOperation();
            
            for (Long userId : batchAssignDTO.getUserIds()) {
                if ("replace".equals(operation)) {
                    // 替换角色：删除现有角色，添加新角色
                    LambdaQueryWrapper<UserRole> deleteWrapper = new LambdaQueryWrapper<>();
                    deleteWrapper.eq(UserRole::getUserId, userId);
                    userRoleService.remove(deleteWrapper);
                    
                    List<UserRole> newUserRoles = batchAssignDTO.getRoleIds().stream().map(roleId -> {
                        UserRole userRole = new UserRole();
                        userRole.setUserId(userId);
                        userRole.setRoleId(roleId);
                        userRole.setCreateTime(new Date());
                        return userRole;
                    }).collect(Collectors.toList());
                    
                    userRoleService.saveBatch(newUserRoles);
                    
                } else if ("add".equals(operation)) {
                    // 新增角色：只添加用户还没有的角色
                    LambdaQueryWrapper<UserRole> existWrapper = new LambdaQueryWrapper<>();
                    existWrapper.eq(UserRole::getUserId, userId)
                               .in(UserRole::getRoleId, batchAssignDTO.getRoleIds());
                    List<UserRole> existingRoles = userRoleService.list(existWrapper);
                    Set<Long> existingRoleIds = existingRoles.stream()
                            .map(UserRole::getRoleId)
                            .collect(Collectors.toSet());
                    
                    List<UserRole> newUserRoles = batchAssignDTO.getRoleIds().stream()
                            .filter(roleId -> !existingRoleIds.contains(roleId))
                            .map(roleId -> {
                                UserRole userRole = new UserRole();
                                userRole.setUserId(userId);
                                userRole.setRoleId(roleId);
                                userRole.setCreateTime(new Date());
                                return userRole;
                            }).collect(Collectors.toList());
                    
                    if (!newUserRoles.isEmpty()) {
                        userRoleService.saveBatch(newUserRoles);
                    }
                    
                } else if ("remove".equals(operation)) {
                    // 移除角色：删除指定的角色
                    LambdaQueryWrapper<UserRole> removeWrapper = new LambdaQueryWrapper<>();
                    removeWrapper.eq(UserRole::getUserId, userId)
                               .in(UserRole::getRoleId, batchAssignDTO.getRoleIds());
                    userRoleService.remove(removeWrapper);
                }
            }
            
            log.info("批量角色分配成功，操作类型: {}, 用户数: {}, 角色数: {}, 备注: {}", 
                    operation, batchAssignDTO.getUserIds().size(), 
                    batchAssignDTO.getRoleIds().size(), batchAssignDTO.getRemark());
            
            return Result.ok();
            
        } catch (Exception e) {
            log.error("批量角色分配失败", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "批量角色分配失败：" + e.getMessage());
        }
    }

    /**
     * 批量角色分配（表单参数版本）
     */
    @Operation(summary = "批量角色分配（表单版）", description = "为多个用户批量分配角色（使用表单参数，方便接口文档测试）")
    @PostMapping("/batch-assign-form")
    public Result<Void> batchAssignRolesForm(
            @Parameter(description = "用户ID列表（用逗号分隔）", required = true)
            @RequestParam @NotNull String userIds,
            @Parameter(description = "角色ID列表（用逗号分隔）", required = true)
            @RequestParam @NotNull String roleIds,
            @Parameter(description = "操作类型：replace-替换，add-增加，remove-移除", required = true)
            @RequestParam @NotNull String operation,
            @Parameter(description = "备注")
            @RequestParam(required = false) String remark) {
        try {
            // 验证操作类型
            if (!Arrays.asList("replace", "add", "remove").contains(operation)) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "操作类型只能为: replace, add, remove");
            }
            
            // 解析用户ID列表
            List<Long> userIdList;
            try {
                userIdList = Arrays.stream(userIds.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::valueOf)
                        .collect(Collectors.toList());
            } catch (NumberFormatException e) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "用户ID格式不正确，请使用逗号分隔的数字");
            }
            
            if (userIdList.isEmpty()) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "用户ID列表不能为空");
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
            
            // 验证用户是否存在
            List<User> users = userService.listByIds(userIdList);
            if (users.size() != userIdList.size()) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "部分用户不存在");
            }
            
            // 验证角色是否存在
            List<Role> roles = roleService.listByIds(roleIdList);
            if (roles.size() != roleIdList.size()) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "部分角色不存在");
            }
            
            for (Long userId : userIdList) {
                if ("replace".equals(operation)) {
                    // 替换角色：删除现有角色，添加新角色
                    LambdaQueryWrapper<UserRole> deleteWrapper = new LambdaQueryWrapper<>();
                    deleteWrapper.eq(UserRole::getUserId, userId);
                    userRoleService.remove(deleteWrapper);
                    
                    List<UserRole> newUserRoles = roleIdList.stream().map(roleId -> {
                        UserRole userRole = new UserRole();
                        userRole.setUserId(userId);
                        userRole.setRoleId(roleId);
                        userRole.setCreateTime(new Date());
                        return userRole;
                    }).collect(Collectors.toList());
                    
                    userRoleService.saveBatch(newUserRoles);
                    
                } else if ("add".equals(operation)) {
                    // 新增角色：只添加用户还没有的角色
                    LambdaQueryWrapper<UserRole> existWrapper = new LambdaQueryWrapper<>();
                    existWrapper.eq(UserRole::getUserId, userId)
                               .in(UserRole::getRoleId, roleIdList);
                    List<UserRole> existingRoles = userRoleService.list(existWrapper);
                    Set<Long> existingRoleIds = existingRoles.stream()
                            .map(UserRole::getRoleId)
                            .collect(Collectors.toSet());
                    
                    List<UserRole> newUserRoles = roleIdList.stream()
                            .filter(roleId -> !existingRoleIds.contains(roleId))
                            .map(roleId -> {
                                UserRole userRole = new UserRole();
                                userRole.setUserId(userId);
                                userRole.setRoleId(roleId);
                                userRole.setCreateTime(new Date());
                                return userRole;
                            }).collect(Collectors.toList());
                    
                    if (!newUserRoles.isEmpty()) {
                        userRoleService.saveBatch(newUserRoles);
                    }
                    
                } else if ("remove".equals(operation)) {
                    // 移除角色：删除指定的角色
                    LambdaQueryWrapper<UserRole> removeWrapper = new LambdaQueryWrapper<>();
                    removeWrapper.eq(UserRole::getUserId, userId)
                               .in(UserRole::getRoleId, roleIdList);
                    userRoleService.remove(removeWrapper);
                }
            }
            
            log.info("批量角色分配成功（表单版），操作类型: {}, 用户数: {}, 角色数: {}, 备注: {}", 
                    operation, userIdList.size(), roleIdList.size(), remark);
            
            return Result.ok();
            
        } catch (Exception e) {
            log.error("批量角色分配失败（表单版）", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "批量角色分配失败：" + e.getMessage());
        }
    }

    /**
     * 删除用户角色关联
     */
    @Operation(summary = "删除用户角色关联", description = "删除指定的用户角色关联")
    @DeleteMapping("/{userId}/role/{roleId}")
    public Result<Void> removeUserRole(
            @Parameter(description = "用户ID", required = true)
            @PathVariable @NotNull @Min(1) Long userId,
            @Parameter(description = "角色ID", required = true)
            @PathVariable @NotNull @Min(1) Long roleId) {
        try {
            // 验证关联是否存在
            LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserRole::getUserId, userId)
                   .eq(UserRole::getRoleId, roleId);
            
            UserRole userRole = userRoleService.getOne(wrapper);
            if (userRole == null) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "用户角色关联不存在");
            }
            
            boolean success = userRoleService.remove(wrapper);
            if (success) {
                log.info("删除用户角色关联成功，userId: {}, roleId: {}", userId, roleId);
                return Result.ok();
            } else {
                return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "删除用户角色关联失败");
            }
            
        } catch (Exception e) {
            log.error("删除用户角色关联失败，userId: {}, roleId: {}", userId, roleId, e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "删除用户角色关联失败：" + e.getMessage());
        }
    }

    /**
     * 获取权限继承关系
     */
    @Operation(summary = "获取权限继承关系", description = "分析用户通过角色获得的权限继承关系")
    @GetMapping("/permission-inheritance")
    public Result<Map<String, Object>> getPermissionInheritance(
            @Parameter(description = "用户ID")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "角色ID")
            @RequestParam(required = false) Long roleId) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            if (userId != null) {
                // 分析指定用户的权限继承
                User user = userService.getById(userId);
                if (user == null) {
                    return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "用户不存在");
                }
                
                // 获取用户所有角色
                LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(UserRole::getUserId, userId);
                List<UserRole> userRoles = userRoleService.list(wrapper);
                
                List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
                List<Role> roles = roleService.listByIds(roleIds);
                
                result.put("userId", userId);
                result.put("username", user.getUsername());
                result.put("nickname", user.getNickname());
                result.put("roles", roles);
                result.put("roleCount", roles.size());
                // TODO: 添加具体权限分析（需要权限表支持）
                result.put("permissions", new ArrayList<>());
                
            } else if (roleId != null) {
                // 分析指定角色的权限和用户
                Role role = roleService.getById(roleId);
                if (role == null) {
                    return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "角色不存在");
                }
                
                // 获取角色下所有用户
                LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(UserRole::getRoleId, roleId);
                List<UserRole> userRoles = userRoleService.list(wrapper);
                
                List<Long> userIds = userRoles.stream().map(UserRole::getUserId).collect(Collectors.toList());
                List<User> users = userService.listByIds(userIds);
                
                result.put("roleId", roleId);
                result.put("roleName", role.getRoleName());
                result.put("roleCode", role.getRoleCode());
                result.put("users", users);
                result.put("userCount", users.size());
                // TODO: 添加具体权限分析（需要权限表支持）
                result.put("permissions", new ArrayList<>());
                
            } else {
                // 返回整体权限继承统计
                result.put("totalUsers", userService.count());
                result.put("totalRoles", roleService.count());
                result.put("totalUserRoles", userRoleService.count());
                
                // 权限继承复杂度分析
                List<UserRole> allUserRoles = userRoleService.list();
                Map<Long, Long> userRoleCount = allUserRoles.stream()
                        .collect(Collectors.groupingBy(UserRole::getUserId, Collectors.counting()));
                Map<Long, Long> roleUserCount = allUserRoles.stream()
                        .collect(Collectors.groupingBy(UserRole::getRoleId, Collectors.counting()));
                
                result.put("avgRolesPerUser", userRoleCount.values().stream()
                        .mapToLong(Long::longValue).average().orElse(0.0));
                result.put("avgUsersPerRole", roleUserCount.values().stream()
                        .mapToLong(Long::longValue).average().orElse(0.0));
                
                // TODO: 添加权限冲突检测、继承链分析等功能
            }
            
            return Result.ok(result);
            
        } catch (Exception e) {
            log.error("获取权限继承关系失败", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "获取权限继承关系失败：" + e.getMessage());
        }
    }
}