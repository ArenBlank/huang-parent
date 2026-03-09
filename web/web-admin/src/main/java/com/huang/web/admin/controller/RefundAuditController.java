package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.service.biz.AdminRefundAuditBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "Admin Refund Audit", description = "Refund records and order/payment reconciliation query")
@RestController
@RequestMapping("/admin/refund")
@RequireAdminRole({AdminRoleCode.ADMIN, AdminRoleCode.AUDIT_ADMIN})
public class RefundAuditController {

    private final AdminRefundAuditBizService adminRefundAuditBizService;

    public RefundAuditController(AdminRefundAuditBizService adminRefundAuditBizService) {
        this.adminRefundAuditBizService = adminRefundAuditBizService;
    }

    @Operation(summary = "Refund audit list (supports status/time/user/order filters)")
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) String refundStatus,
                          @RequestParam(required = false) Long userId,
                          @RequestParam(required = false) String orderNo,
                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                          @RequestParam(defaultValue = "50") Integer limit) {
        return Result.ok(adminRefundAuditBizService.listRefundAudits(refundStatus, userId, orderNo, startTime, endTime, limit));
    }
}
