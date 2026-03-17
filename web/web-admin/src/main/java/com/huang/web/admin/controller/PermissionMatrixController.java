package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.web.admin.custom.annotation.RequireAdminPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.model.entity.Permission;
import com.huang.web.admin.custom.config.AdminPermissionMatrixValidator;
import com.huang.web.admin.custom.config.AdminPermissionMatrixView;
import com.huang.web.admin.custom.aop.OperationLog;
import com.huang.web.admin.mapper.PermissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Tag(name = "Admin Permission Matrix", description = "Permission matrix diagnostics")
@RestController
@RequestMapping("/admin/permission")
public class PermissionMatrixController {

    private final AdminPermissionMatrixValidator matrixValidator;
    private final PermissionMapper permissionMapper;

    public PermissionMatrixController(AdminPermissionMatrixValidator matrixValidator,
                                      PermissionMapper permissionMapper) {
        this.matrixValidator = matrixValidator;
        this.permissionMapper = permissionMapper;
    }

    @Operation(summary = "Get permission matrix diagnostics")
    @GetMapping("/matrix")
    @RequireAdminPermission({"operation:log:read"})
    public Result<AdminPermissionMatrixView> matrix() {
        AdminPermissionMatrixView view = matrixValidator.buildView();
        if (view.getMissing() != null && !view.getMissing().isEmpty()) {
            view.setWarning("missing permissions in matrix: " + view.getMissing().size());
        }
        return Result.ok(view);
    }

    @Operation(summary = "Sync missing permissions into database")
    @PostMapping("/matrix/sync")
    @RequireAdminPermission({"role:permission"})
    @OperationLog(module = "permission_matrix", action = "sync", detail = "admin sync missing permissions")
    public Result<List<String>> syncMissing() {
        AdminPermissionMatrixView view = matrixValidator.buildView();
        Set<String> missing = view.getMissing();
        if (missing == null || missing.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }
        List<String> synced = new ArrayList<>();
        for (String permCode : missing) {
            if (permCode == null || permCode.isBlank()) {
                continue;
            }
            Permission exists = permissionMapper.selectOne(new LambdaQueryWrapper<Permission>()
                    .eq(Permission::getPermCode, permCode)
                    .last("LIMIT 1"));
            if (exists != null) {
                continue;
            }
            Permission permission = new Permission();
            permission.setPermCode(permCode);
            permission.setPermName(permCode);
            permission.setModule(extractModule(permCode));
            permission.setStatus(1);
            permissionMapper.insert(permission);
            synced.add(permCode);
        }
        return Result.ok(synced);
    }

    private String extractModule(String permCode) {
        int idx = permCode.indexOf(':');
        if (idx > 0) {
            return permCode.substring(0, idx);
        }
        return permCode;
    }
}
