package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.web.admin.constant.AdminErrorCode;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminPermission;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.custom.aop.OperationLog;
import com.huang.web.admin.dto.config.SystemConfigUpsertDTO;
import com.huang.web.admin.service.biz.AdminSystemConfigBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin系统配置", description = "系统参数配置管理")
@RestController
@RequestMapping("/admin/system-config")
@RequireAdminRole(AdminRoleCode.ADMIN)
public class SystemConfigManageController {

    private final AdminSystemConfigBizService adminSystemConfigBizService;

    public SystemConfigManageController(AdminSystemConfigBizService adminSystemConfigBizService) {
        this.adminSystemConfigBizService = adminSystemConfigBizService;
    }

    @Operation(summary = "配置列表")
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) String keyLike) {
        return Result.ok(adminSystemConfigBizService.list(keyLike));
    }

    @Operation(summary = "新增配置")
    @PostMapping
    @RequireAdminPermission({"system:config"})
    @OperationLog(module = "system_config", action = "create", detail = "admin create system config")
    public Result<?> create(@Valid @RequestBody SystemConfigUpsertDTO dto) {
        Long id = adminSystemConfigBizService.create(dto);
        return id == null ? Result.fail(AdminErrorCode.SYSTEM_CONFIG_KEY_DUPLICATE, "配置键已存在") : Result.ok(id);
    }

    @Operation(summary = "更新配置")
    @PutMapping("/{id}")
    @RequireAdminPermission({"system:config"})
    @OperationLog(module = "system_config", action = "update", detail = "admin update system config")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody SystemConfigUpsertDTO dto) {
        boolean ok = adminSystemConfigBizService.update(id, dto);
        return ok ? Result.ok("更新成功") : Result.fail(AdminErrorCode.SYSTEM_CONFIG_UPDATE_FAILED, "更新失败：记录不存在或配置键重复");
    }

    @Operation(summary = "删除配置")
    @DeleteMapping("/{id}")
    @RequireAdminPermission({"system:config"})
    @OperationLog(module = "system_config", action = "delete", detail = "admin delete system config")
    public Result<?> delete(@PathVariable Long id) {
        boolean ok = adminSystemConfigBizService.delete(id);
        return ok ? Result.ok("删除成功") : Result.fail(AdminErrorCode.SYSTEM_CONFIG_NOT_FOUND, "配置不存在");
    }
}
