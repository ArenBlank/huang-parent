package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.result.Result;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.model.entity.User;
import com.huang.model.entity.UserRole;
import com.huang.web.admin.dto.user.UserRoleAssignDTO;
import com.huang.web.admin.dto.user.UserStatusUpdateDTO;
import com.huang.web.admin.service.UserRoleService;
import com.huang.web.admin.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin用户管理", description = "用户查询与状态维护")
@RestController
@RequestMapping("/admin/user")
@RequireAdminRole(AdminRoleCode.ADMIN)
public class UserController {

    private final UserService userService;
    private final UserRoleService userRoleService;

    public UserController(UserService userService, UserRoleService userRoleService) {
        this.userService = userService;
        this.userRoleService = userRoleService;
    }

    @Operation(summary = "用户列表")
    @GetMapping("/list")
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

    @Operation(summary = "用户详情")
    @GetMapping("/detail/{userId}")
    public Result<User> detail(@PathVariable Long userId) {
        User user = userService.getById(userId);
        return user == null ? Result.fail("用户不存在") : Result.ok(user);
    }

    @Operation(summary = "更新用户状态")
    @PutMapping("/status")
    public Result<String> updateStatus(@Valid @RequestBody UserStatusUpdateDTO dto) {
        User user = userService.getById(dto.getUserId());
        if (user == null) {
            return Result.fail("用户不存在");
        }
        user.setStatus(dto.getStatus());
        return userService.updateById(user) ? Result.ok("更新成功") : Result.fail("更新失败");
    }

    @Operation(summary = "给用户分配角色")
    @PostMapping("/assign-roles")
    public Result<String> assignRoles(@Valid @RequestBody UserRoleAssignDTO dto) {
        if (userService.getById(dto.getUserId()) == null) {
            return Result.fail("用户不存在");
        }
        userRoleService.remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, dto.getUserId()));
        for (Long roleId : dto.getRoleIds()) {
            UserRole ur = new UserRole();
            ur.setUserId(dto.getUserId());
            ur.setRoleId(roleId);
            userRoleService.save(ur);
        }
        return Result.ok("分配成功");
    }
}
