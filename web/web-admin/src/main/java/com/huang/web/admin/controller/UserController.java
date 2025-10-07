package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.common.result.Result;
import com.huang.common.result.ResultCodeEnum;
import com.huang.web.admin.dto.user.UserQueryDTO;
import com.huang.web.admin.dto.user.UserStatusUpdateDTO;
import com.huang.web.admin.dto.user.UserRoleAssignDTO;
import com.huang.web.admin.service.AdminUserService;
import com.huang.web.admin.vo.user.UserListVO;
import com.huang.web.admin.vo.user.UserDetailVO;
import com.huang.web.admin.vo.user.UserStatisticsVO;
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
import java.util.ArrayList;
import java.util.List;

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
    private AdminUserService adminUserService;

    /**
     * 用户列表查询（分页、筛选）
     */
    @Operation(summary = "用户列表查询", description = "支持分页和多条件筛选的用户列表查询")
    @GetMapping("/list")
    public Result<Page<UserListVO>> getUserList(@Valid UserQueryDTO queryDTO) {
        try {
            Page<UserListVO> resultPage = adminUserService.getUserList(queryDTO);
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
            UserDetailVO detailVO = adminUserService.getUserDetail(userId);
            if (detailVO == null) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "用户不存在");
            }
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
            boolean success = adminUserService.updateUserStatus(statusUpdateDTO);
            if (success) {
                return Result.ok();
            } else {
                return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "更新用户状态失败");
            }
        } catch (RuntimeException e) {
            log.error("更新用户状态失败: {}", e.getMessage());
            return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), e.getMessage());
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
            
            UserStatusUpdateDTO statusUpdateDTO = new UserStatusUpdateDTO();
            statusUpdateDTO.setUserId(userId);
            statusUpdateDTO.setStatus(status);
            statusUpdateDTO.setRemark(remark);
            
            boolean success = adminUserService.updateUserStatus(statusUpdateDTO);
            if (success) {
                return Result.ok();
            } else {
                return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "更新用户状态失败");
            }
        } catch (RuntimeException e) {
            log.error("更新用户状态失败: {}", e.getMessage());
            return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), e.getMessage());
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
            boolean success = adminUserService.assignUserRoles(assignDTO);
            if (success) {
                return Result.ok();
            } else {
                return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "用户角色分配失败");
            }
        } catch (RuntimeException e) {
            log.error("用户角色分配失败: {}", e.getMessage());
            return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), e.getMessage());
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
            // 解析角色ID列表
            String[] roleIdArray = roleIds.split(",");
            if (roleIdArray.length == 0) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "角色ID列表不能为空");
            }
            
            // 转换String数组为List<Long>
            List<Long> roleIdList = new ArrayList<>();
            for (String roleIdStr : roleIdArray) {
                try {
                    Long roleId = Long.parseLong(roleIdStr.trim());
                    if (roleId > 0) {
                        roleIdList.add(roleId);
                    }
                } catch (NumberFormatException e) {
                    return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), 
                        "无效的角色ID格式: " + roleIdStr);
                }
            }
            
            if (roleIdList.isEmpty()) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "没有有效的角色ID");
            }
            
            UserRoleAssignDTO assignDTO = new UserRoleAssignDTO();
            assignDTO.setUserId(userId);
            assignDTO.setRoleIds(roleIdList);
            assignDTO.setRemark(remark);
            
            boolean success = adminUserService.assignUserRoles(assignDTO);
            if (success) {
                return Result.ok();
            } else {
                return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "用户角色分配失败");
            }
        } catch (RuntimeException e) {
            log.error("用户角色分配失败: {}", e.getMessage());
            return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), e.getMessage());
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
            UserStatisticsVO statisticsVO = adminUserService.getUserStatistics();
            return Result.ok(statisticsVO);
        } catch (Exception e) {
            log.error("获取用户统计数据失败", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "获取用户统计数据失败：" + e.getMessage());
        }
    }
}