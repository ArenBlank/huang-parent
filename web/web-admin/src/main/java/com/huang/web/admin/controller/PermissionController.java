package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.model.entity.Permission;
import com.huang.web.admin.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理Controller(模拟实现)
 */
@Tag(name = "权限管理")
@RestController
@RequestMapping("/admin/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "根据角色ID查询权限列表")
    @GetMapping("/role/{roleId}")
    public Result<List<Permission>> getPermissionsByRoleId(@PathVariable Long roleId) {
        List<Permission> permissions = permissionService.getPermissionsByRoleId(roleId);
        return Result.ok(permissions);
    }

    @Operation(summary = "根据用户ID查询权限列表")
    @GetMapping("/user/{userId}")
    public Result<List<Permission>> getPermissionsByUserId(@PathVariable Long userId) {
        List<Permission> permissions = permissionService.getPermissionsByUserId(userId);
        return Result.ok(permissions);
    }

    @Operation(summary = "检查用户是否有指定权限")
    @GetMapping("/check")
    public Result<Boolean> hasPermission(@RequestParam Long userId,
                                          @RequestParam String permissionCode) {
        boolean has = permissionService.hasPermission(userId, permissionCode);
        return Result.ok(has);
    }
}
