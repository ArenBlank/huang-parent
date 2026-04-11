package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.result.Result;
import com.huang.model.entity.UserRole;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminPermission;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.custom.aop.OperationLog;
import com.huang.web.admin.dto.userrole.BatchRoleAssignDTO;
import com.huang.web.admin.service.UserRoleService;
import com.huang.web.admin.service.biz.auth.AdminUserRoleBindingBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin User Role Binding", description = "User role binding management")
@RestController
@RequestMapping("/admin/user-role")
@RequireAdminRole(AdminRoleCode.ADMIN)
public class UserRoleController {

    private final UserRoleService userRoleService;
    private final AdminUserRoleBindingBizService adminUserRoleBindingBizService;

    public UserRoleController(UserRoleService userRoleService,
                              AdminUserRoleBindingBizService adminUserRoleBindingBizService) {
        this.userRoleService = userRoleService;
        this.adminUserRoleBindingBizService = adminUserRoleBindingBizService;
    }

    @Operation(summary = "List user-role bindings")
    @GetMapping("/list")
    @RequireAdminPermission({"user:role"})
    public Result<List<UserRole>> list() {
        return Result.ok(userRoleService.list(new LambdaQueryWrapper<UserRole>().orderByDesc(UserRole::getId)));
    }

    @Operation(summary = "List roles by user")
    @GetMapping("/user/{userId}/roles")
    @RequireAdminPermission({"user:role"})
    public Result<List<UserRole>> userRoles(@PathVariable Long userId) {
        return Result.ok(userRoleService.list(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)));
    }

    @Operation(summary = "List users by role")
    @GetMapping("/role/{roleId}/users")
    @RequireAdminPermission({"user:role"})
    public Result<List<UserRole>> roleUsers(@PathVariable Long roleId) {
        return Result.ok(userRoleService.list(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId)));
    }

    @Operation(summary = "Batch assign roles")
    @PostMapping("/batch-assign")
    @RequireAdminPermission({"user:role"})
    @OperationLog(module = "user_role", action = "batch_assign", detail = "admin batch assign roles")
    public Result<String> batchAssign(@Valid @RequestBody BatchRoleAssignDTO dto) {
        return adminUserRoleBindingBizService.batchAssign(dto)
                ? Result.ok("Success")
                : Result.fail("operation only supports replace/add/remove");
    }

    @Operation(summary = "Remove user-role binding")
    @DeleteMapping("/{userId}/role/{roleId}")
    @RequireAdminPermission({"user:role"})
    @OperationLog(module = "user_role", action = "remove", detail = "admin remove user role")
    public Result<String> remove(@PathVariable Long userId, @PathVariable Long roleId) {
        boolean ok = adminUserRoleBindingBizService.removeRole(userId, roleId);
        return ok ? Result.ok("Remove success") : Result.fail("Binding not found");
    }
}
