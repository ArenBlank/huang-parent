package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.result.Result;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.model.entity.Role;
import com.huang.web.admin.dto.role.RoleStatusUpdateDTO;
import com.huang.web.admin.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin角色管理", description = "角色查询与状态维护")
@RestController
@RequestMapping("/admin/role")
@RequireAdminRole(AdminRoleCode.ADMIN)
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "角色列表")
    @GetMapping("/list")
    public Result<List<Role>> list(@RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Role::getStatus, status);
        }
        wrapper.orderByDesc(Role::getId);
        return Result.ok(roleService.list(wrapper));
    }

    @Operation(summary = "角色详情")
    @GetMapping("/detail/{roleId}")
    public Result<Role> detail(@PathVariable Long roleId) {
        Role role = roleService.getById(roleId);
        return role == null ? Result.fail("角色不存在") : Result.ok(role);
    }

    @Operation(summary = "更新角色状态")
    @PutMapping("/status")
    public Result<String> updateStatus(@Valid @RequestBody RoleStatusUpdateDTO dto) {
        Role role = roleService.getById(dto.getRoleId());
        if (role == null) {
            return Result.fail("角色不存在");
        }
        role.setStatus(dto.getStatus());
        return roleService.updateById(role) ? Result.ok("更新成功") : Result.fail("更新失败");
    }

    @Operation(summary = "可用角色列表")
    @GetMapping("/available")
    public Result<List<Role>> available() {
        return Result.ok(roleService.list(new LambdaQueryWrapper<Role>()
                .eq(Role::getStatus, 1)
                .orderByAsc(Role::getId)));
    }
}
