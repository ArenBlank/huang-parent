package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.web.admin.constant.AdminErrorCode;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminPermission;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.custom.aop.OperationLog;
import com.huang.web.admin.dto.coach.CoachApplyAuditDTO;
import com.huang.web.admin.service.biz.AdminCoachApplyBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin教练申请")
@RestController
@RequestMapping("/admin/coach-apply")
@RequireAdminRole({AdminRoleCode.ADMIN, AdminRoleCode.AUDIT_ADMIN})
public class CoachApplyManageController {

    private final AdminCoachApplyBizService adminCoachApplyBizService;

    public CoachApplyManageController(AdminCoachApplyBizService adminCoachApplyBizService) {
        this.adminCoachApplyBizService = adminCoachApplyBizService;
    }

    @Operation(summary = "教练申请列表")
    @GetMapping("/list")
    @RequireAdminPermission({"coach:apply:audit"})
    public Result<?> list(@RequestParam(required = false) Integer certStatus,
                          @RequestParam(required = false) String keyword) {
        return Result.ok(adminCoachApplyBizService.list(certStatus, keyword));
    }

    @Operation(summary = "教练申请详情")
    @GetMapping("/{profileId}")
    @RequireAdminPermission({"coach:apply:audit"})
    public Result<?> detail(@PathVariable Long profileId) {
        return Result.ok(adminCoachApplyBizService.detail(profileId));
    }

    @Operation(summary = "审核教练申请")
    @PostMapping("/audit")
    @RequireAdminPermission({"coach:apply:audit"})
    @OperationLog(module = "coach_apply", action = "audit", detail = "admin audit coach apply")
    public Result<?> audit(@Valid @RequestBody CoachApplyAuditDTO dto) {
        boolean ok = adminCoachApplyBizService.audit(dto);
        return ok ? Result.ok() : Result.fail(AdminErrorCode.COACH_APPLY_AUDIT_FAILED, "审核失败：申请不存在、状态不合法或已被并发处理");
    }
}
