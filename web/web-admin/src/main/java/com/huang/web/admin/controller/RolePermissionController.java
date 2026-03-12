package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.model.entity.Permission;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.dto.role.RolePermissionAssignDTO;
import com.huang.web.admin.service.biz.AdminRolePermissionBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin Role Permission", description = "Role-permission management")
@RestController
@RequestMapping("/admin/role-permission")
@RequireAdminRole(AdminRoleCode.ADMIN)
public class RolePermissionController {

    private final AdminRolePermissionBizService adminRolePermissionBizService;

    public RolePermissionController(AdminRolePermissionBizService adminRolePermissionBizService) {
        this.adminRolePermissionBizService = adminRolePermissionBizService;
    }

    @Operation(summary = "List permissions")
    @GetMapping("/permissions")
    public Result<List<Permission>> listPermissions() {
        return Result.ok(adminRolePermissionBizService.listPermissions());
    }

    @Operation(summary = "List role permissions")
    @GetMapping("/role/{roleId}/permissions")
    public Result<List<Permission>> listRolePermissions(@PathVariable Long roleId) {
        return Result.ok(adminRolePermissionBizService.listPermissionsByRole(roleId));
    }

    @Operation(summary = "Assign role permissions (replace/add/remove)")
    @PostMapping("/assign")
    public Result<String> assign(@Valid @RequestBody RolePermissionAssignDTO dto) {
        boolean ok = adminRolePermissionBizService.assign(dto);
        return ok ? Result.ok("success") : Result.fail("assign failed");
    }
}
