package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.web.admin.custom.annotation.RequireAdminPermission;
import com.huang.web.admin.custom.config.AdminPermissionMatrixValidator;
import com.huang.web.admin.custom.config.AdminPermissionMatrixView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Permission Matrix", description = "Permission matrix diagnostics")
@RestController
@RequestMapping("/admin/permission")
public class PermissionMatrixController {

    private final AdminPermissionMatrixValidator matrixValidator;

    public PermissionMatrixController(AdminPermissionMatrixValidator matrixValidator) {
        this.matrixValidator = matrixValidator;
    }

    @Operation(summary = "Get permission matrix diagnostics")
    @GetMapping("/matrix")
    @RequireAdminPermission({"operation:log:read"})
    public Result<AdminPermissionMatrixView> matrix() {
        return Result.ok(matrixValidator.buildView());
    }
}
