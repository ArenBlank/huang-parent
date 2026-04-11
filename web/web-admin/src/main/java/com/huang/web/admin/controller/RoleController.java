package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.result.Result;
import com.huang.model.entity.Role;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminPermission;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.custom.aop.OperationLog;
import com.huang.web.admin.dto.role.RoleCourseCategoryScopeBatchUpdateDTO;
import com.huang.web.admin.dto.role.RoleCourseCategoryScopeUpdateDTO;
import com.huang.web.admin.dto.role.RoleStatusUpdateDTO;
import com.huang.web.admin.service.RoleService;
import com.huang.web.admin.service.biz.AdminRoleScopeBizService;
import com.huang.web.admin.service.biz.auth.AdminRoleAclBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin Role Management", description = "Role query and maintenance")
@RestController
@RequestMapping("/admin/role")
@RequireAdminRole(AdminRoleCode.ADMIN)
public class RoleController {

    private final RoleService roleService;
    private final AdminRoleScopeBizService adminRoleScopeBizService;
    private final AdminRoleAclBizService adminRoleAclBizService;

    public RoleController(RoleService roleService,
                          AdminRoleScopeBizService adminRoleScopeBizService,
                          AdminRoleAclBizService adminRoleAclBizService) {
        this.roleService = roleService;
        this.adminRoleScopeBizService = adminRoleScopeBizService;
        this.adminRoleAclBizService = adminRoleAclBizService;
    }

    @Operation(summary = "List roles")
    @RequireAdminPermission({"role:read"})
    @GetMapping("/list")
    public Result<List<Role>> list(@RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Role::getStatus, status);
        }
        wrapper.orderByDesc(Role::getId);
        return Result.ok(roleService.list(wrapper));
    }

    @Operation(summary = "Role detail")
    @RequireAdminPermission({"role:read"})
    @GetMapping("/detail/{roleId}")
    public Result<Role> detail(@PathVariable Long roleId) {
        Role role = roleService.getById(roleId);
        return role == null ? Result.fail("Role not found") : Result.ok(role);
    }

    @Operation(summary = "Update role status")
    @RequireAdminPermission({"role:status"})
    @OperationLog(module = "role", action = "update_status", detail = "admin update role status")
    @PutMapping("/status")
    public Result<String> updateStatus(@Valid @RequestBody RoleStatusUpdateDTO dto) {
        Role role = roleService.getById(dto.getRoleId());
        if (role == null) {
            return Result.fail("Role not found");
        }
        return adminRoleAclBizService.updateStatus(dto) ? Result.ok("Update success") : Result.fail("Update failed");
    }

    @Operation(summary = "List active roles")
    @RequireAdminPermission({"role:read"})
    @GetMapping("/available")
    public Result<List<Role>> available() {
        return Result.ok(roleService.list(new LambdaQueryWrapper<Role>()
                .eq(Role::getStatus, 1)
                .orderByAsc(Role::getId)));
    }

    @Operation(summary = "Get role course category scope")
    @RequireAdminPermission({"role:scope:read"})
    @GetMapping("/{roleId}/course-category-scope")
    public Result<List<Long>> courseCategoryScope(@PathVariable Long roleId) {
        Role role = roleService.getById(roleId);
        if (role == null) {
            return Result.fail("Role not found");
        }
        return Result.ok(adminRoleScopeBizService.listCourseCategoryScope(roleId));
    }

    @Operation(summary = "Update role course category scope")
    @OperationLog(module = "role_scope", action = "update", detail = "admin update role course category scope")
    @RequireAdminPermission({"role:scope:update"})
    @PutMapping("/{roleId}/course-category-scope")
    public Result<String> updateCourseCategoryScope(@PathVariable Long roleId,
                                                    @Valid @RequestBody RoleCourseCategoryScopeUpdateDTO dto) {
        boolean ok = adminRoleScopeBizService.updateCourseCategoryScope(roleId, dto.getCategoryIds());
        return ok ? Result.ok("Update success") : Result.fail("Role not found");
    }

    @Operation(summary = "Batch update role course category scope")
    @OperationLog(module = "role_scope", action = "batch_update", detail = "admin batch update role course category scope")
    @RequireAdminPermission({"role:scope:update"})
    @PutMapping("/course-category-scope/batch")
    public Result<String> updateCourseCategoryScopeBatch(@Valid @RequestBody RoleCourseCategoryScopeBatchUpdateDTO dto) {
        boolean ok = adminRoleScopeBizService.updateCourseCategoryScopeBatch(dto.getItems());
        return ok ? Result.ok("Update success") : Result.fail("Role not found");
    }
}
