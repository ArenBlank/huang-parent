package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.service.biz.AdminPaymentCallbackAuditBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "Admin Payment Callback Audit", description = "Async callback processing audit query")
@RestController
@RequestMapping("/admin/pay/callback")
@RequireAdminRole({AdminRoleCode.ADMIN, AdminRoleCode.AUDIT_ADMIN})
public class PaymentCallbackAuditController {

    private final AdminPaymentCallbackAuditBizService adminPaymentCallbackAuditBizService;

    public PaymentCallbackAuditController(AdminPaymentCallbackAuditBizService adminPaymentCallbackAuditBizService) {
        this.adminPaymentCallbackAuditBizService = adminPaymentCallbackAuditBizService;
    }

    @Operation(summary = "Callback audit list (supports payNo/tradeNo/result/time filters)")
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) String payNo,
                          @RequestParam(required = false) String tradeNo,
                          @RequestParam(required = false) String processResult,
                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                          @RequestParam(defaultValue = "50") Integer limit) {
        return Result.ok(adminPaymentCallbackAuditBizService.list(payNo, tradeNo, processResult, startTime, endTime, limit));
    }
}
