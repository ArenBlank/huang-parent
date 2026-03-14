package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.web.admin.constant.AdminErrorCode;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminPermission;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.custom.aop.OperationLog;
import com.huang.web.admin.dto.notice.NoticeUpsertDTO;
import com.huang.web.admin.service.biz.AdminNoticeBizService;
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

@Tag(name = "Admin运营公告", description = "公告管理")
@RestController
@RequestMapping("/admin/notice")
@RequireAdminRole({AdminRoleCode.ADMIN, AdminRoleCode.OPS_ADMIN})
public class NoticeManageController {

    private final AdminNoticeBizService adminNoticeBizService;

    public NoticeManageController(AdminNoticeBizService adminNoticeBizService) {
        this.adminNoticeBizService = adminNoticeBizService;
    }

    @Operation(summary = "公告列表")
    @RequireAdminPermission({"notice:manage"})
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) Integer status) {
        return Result.ok(adminNoticeBizService.list(status));
    }

    @Operation(summary = "新增公告")
    @PostMapping
    @RequireAdminPermission({"notice:manage"})
    @OperationLog(module = "notice", action = "create", detail = "admin create notice")
    public Result<?> create(@Valid @RequestBody NoticeUpsertDTO dto) {
        return Result.ok(adminNoticeBizService.create(dto));
    }

    @Operation(summary = "更新公告")
    @PutMapping("/{id}")
    @RequireAdminPermission({"notice:manage"})
    @OperationLog(module = "notice", action = "update", detail = "admin update notice")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody NoticeUpsertDTO dto) {
        boolean ok = adminNoticeBizService.update(id, dto);
        return ok ? Result.ok("更新成功") : Result.fail(AdminErrorCode.NOTICE_NOT_FOUND, "公告不存在");
    }

    @Operation(summary = "更新公告状态")
    @PutMapping("/{id}/status")
    @RequireAdminPermission({"notice:manage"})
    @OperationLog(module = "notice", action = "update_status", detail = "admin update notice status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return Result.fail(AdminErrorCode.NOTICE_STATUS_INVALID, "公告状态仅支持 0/1");
        }
        boolean ok = adminNoticeBizService.updateStatus(id, status);
        return ok ? Result.ok("状态更新成功") : Result.fail(AdminErrorCode.NOTICE_NOT_FOUND, "公告不存在");
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    @RequireAdminPermission({"notice:manage"})
    @OperationLog(module = "notice", action = "delete", detail = "admin delete notice")
    public Result<?> delete(@PathVariable Long id) {
        boolean ok = adminNoticeBizService.delete(id);
        return ok ? Result.ok("删除成功") : Result.fail(AdminErrorCode.NOTICE_NOT_FOUND, "公告不存在");
    }
}
