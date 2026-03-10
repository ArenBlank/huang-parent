package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.web.admin.constant.AdminErrorCode;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminPermission;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.custom.aop.OperationLog;
import com.huang.web.admin.dto.banner.BannerUpsertDTO;
import com.huang.web.admin.service.biz.AdminBannerBizService;
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

@Tag(name = "Admin运营Banner", description = "首页轮播图管理")
@RestController
@RequestMapping("/admin/banner")
@RequireAdminRole({AdminRoleCode.ADMIN, AdminRoleCode.OPS_ADMIN})
public class BannerManageController {

    private final AdminBannerBizService adminBannerBizService;

    public BannerManageController(AdminBannerBizService adminBannerBizService) {
        this.adminBannerBizService = adminBannerBizService;
    }

    @Operation(summary = "Banner列表")
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) Integer status) {
        return Result.ok(adminBannerBizService.list(status));
    }

    @Operation(summary = "新增Banner")
    @PostMapping
    @RequireAdminPermission({"banner:manage"})
    @OperationLog(module = "banner", action = "create", detail = "admin create banner")
    public Result<?> create(@Valid @RequestBody BannerUpsertDTO dto) {
        return Result.ok(adminBannerBizService.create(dto));
    }

    @Operation(summary = "更新Banner")
    @PutMapping("/{id}")
    @RequireAdminPermission({"banner:manage"})
    @OperationLog(module = "banner", action = "update", detail = "admin update banner")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody BannerUpsertDTO dto) {
        boolean ok = adminBannerBizService.update(id, dto);
        return ok ? Result.ok("更新成功") : Result.fail(AdminErrorCode.BANNER_NOT_FOUND, "Banner不存在");
    }

    @Operation(summary = "更新Banner状态")
    @PutMapping("/{id}/status")
    @RequireAdminPermission({"banner:manage"})
    @OperationLog(module = "banner", action = "update_status", detail = "admin update banner status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return Result.fail(AdminErrorCode.BANNER_STATUS_INVALID, "Banner状态仅支持 0/1");
        }
        boolean ok = adminBannerBizService.updateStatus(id, status);
        return ok ? Result.ok("状态更新成功") : Result.fail(AdminErrorCode.BANNER_NOT_FOUND, "Banner不存在");
    }

    @Operation(summary = "删除Banner")
    @DeleteMapping("/{id}")
    @RequireAdminPermission({"banner:manage"})
    @OperationLog(module = "banner", action = "delete", detail = "admin delete banner")
    public Result<?> delete(@PathVariable Long id) {
        boolean ok = adminBannerBizService.delete(id);
        return ok ? Result.ok("删除成功") : Result.fail(AdminErrorCode.BANNER_NOT_FOUND, "Banner不存在");
    }
}
