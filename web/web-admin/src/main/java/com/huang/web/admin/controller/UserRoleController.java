package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.result.Result;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.model.entity.UserRole;
import com.huang.web.admin.dto.userrole.BatchRoleAssignDTO;
import com.huang.web.admin.service.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin用户角色关系", description = "用户与角色关联关系维护")
@RestController
@RequestMapping("/admin/user-role")
@RequireAdminRole(AdminRoleCode.ADMIN)
public class UserRoleController {

    private final UserRoleService userRoleService;

    public UserRoleController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    @Operation(summary = "用户角色关系列表")
    @GetMapping("/list")
    public Result<List<UserRole>> list() {
        return Result.ok(userRoleService.list(new LambdaQueryWrapper<UserRole>().orderByDesc(UserRole::getId)));
    }

    @Operation(summary = "查询用户角色")
    @GetMapping("/user/{userId}/roles")
    public Result<List<UserRole>> userRoles(@PathVariable Long userId) {
        return Result.ok(userRoleService.list(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)));
    }

    @Operation(summary = "查询角色用户")
    @GetMapping("/role/{roleId}/users")
    public Result<List<UserRole>> roleUsers(@PathVariable Long roleId) {
        return Result.ok(userRoleService.list(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId)));
    }

    @Operation(summary = "批量分配角色")
    @PostMapping("/batch-assign")
    public Result<String> batchAssign(@Valid @RequestBody BatchRoleAssignDTO dto) {
        String operation = dto.getOperation() == null ? "replace" : dto.getOperation().trim().toLowerCase();
        for (Long userId : dto.getUserIds()) {
            if ("replace".equals(operation)) {
                userRoleService.remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
                for (Long roleId : dto.getRoleIds()) {
                    UserRole ur = new UserRole();
                    ur.setUserId(userId);
                    ur.setRoleId(roleId);
                    userRoleService.save(ur);
                }
            } else if ("add".equals(operation)) {
                for (Long roleId : dto.getRoleIds()) {
                    long count = userRoleService.count(new LambdaQueryWrapper<UserRole>()
                            .eq(UserRole::getUserId, userId)
                            .eq(UserRole::getRoleId, roleId));
                    if (count == 0) {
                        UserRole ur = new UserRole();
                        ur.setUserId(userId);
                        ur.setRoleId(roleId);
                        userRoleService.save(ur);
                    }
                }
            } else if ("remove".equals(operation)) {
                userRoleService.remove(new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId)
                        .in(UserRole::getRoleId, dto.getRoleIds()));
            } else {
                return Result.fail("operation 仅支持 replace/add/remove");
            }
        }
        return Result.ok("处理成功");
    }

    @Operation(summary = "删除用户角色关联")
    @DeleteMapping("/{userId}/role/{roleId}")
    public Result<String> remove(@PathVariable Long userId, @PathVariable Long roleId) {
        boolean ok = userRoleService.remove(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, roleId));
        return ok ? Result.ok("删除成功") : Result.fail("关联不存在");
    }
}
