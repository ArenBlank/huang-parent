package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.result.Result;
import com.huang.model.entity.User;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminPermission;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.custom.aop.OperationLog;
import com.huang.web.admin.dto.user.UserRoleAssignDTO;
import com.huang.web.admin.dto.user.UserStatusUpdateDTO;
import com.huang.web.admin.service.UserService;
import com.huang.web.admin.service.biz.auth.AdminUserAuthBizService;
import com.huang.web.admin.service.biz.auth.AdminUserRoleBindingBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin User Management", description = "User query and maintenance")
@RestController
@RequestMapping("/admin/user")
@RequireAdminRole(AdminRoleCode.ADMIN)
public class UserController {

    private final UserService userService;
    private final AdminUserAuthBizService adminUserAuthBizService;
    private final AdminUserRoleBindingBizService adminUserRoleBindingBizService;

    public UserController(UserService userService,
                          AdminUserAuthBizService adminUserAuthBizService,
                          AdminUserRoleBindingBizService adminUserRoleBindingBizService) {
        this.userService = userService;
        this.adminUserAuthBizService = adminUserAuthBizService;
        this.adminUserRoleBindingBizService = adminUserRoleBindingBizService;
    }

    @Operation(summary = "List users")
    @GetMapping("/list")
    @RequireAdminPermission({"user:read"})
    public Result<List<User>> list(@RequestParam(required = false) Integer status,
                                   @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getPhone, keyword));
        }
        wrapper.orderByDesc(User::getId);
        return Result.ok(userService.list(wrapper));
    }

    @Operation(summary = "User detail")
    @GetMapping("/detail/{userId}")
    @RequireAdminPermission({"user:read"})
    public Result<User> detail(@PathVariable Long userId) {
        User user = userService.getById(userId);
        return user == null ? Result.fail("User not found") : Result.ok(user);
    }

    @Operation(summary = "Update user status")
    @PutMapping("/status")
    @RequireAdminPermission({"user:status"})
    @OperationLog(module = "user", action = "update_status", detail = "admin update user status")
    public Result<String> updateStatus(@Valid @RequestBody UserStatusUpdateDTO dto) {
        User user = userService.getById(dto.getUserId());
        if (user == null) {
            return Result.fail("User not found");
        }
        return adminUserAuthBizService.updateStatus(dto) ? Result.ok("Update success") : Result.fail("Update failed");
    }

    @Operation(summary = "Assign user roles")
    @PostMapping("/assign-roles")
    @RequireAdminPermission({"user:role"})
    @OperationLog(module = "user_role", action = "assign", detail = "admin assign roles to user")
    public Result<String> assignRoles(@Valid @RequestBody UserRoleAssignDTO dto) {
        if (userService.getById(dto.getUserId()) == null) {
            return Result.fail("User not found");
        }
        return adminUserRoleBindingBizService.assignRoles(dto) ? Result.ok("Assign success") : Result.fail("Assign failed");
    }
}
