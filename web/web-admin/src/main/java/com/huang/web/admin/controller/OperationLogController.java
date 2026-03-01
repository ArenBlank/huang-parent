package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.web.admin.service.biz.AdminOperationLogBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin操作日志", description = "关键写操作审计日志查询")
@RestController
@RequestMapping("/admin/operation-log")
public class OperationLogController {

    private final AdminOperationLogBizService adminOperationLogBizService;

    public OperationLogController(AdminOperationLogBizService adminOperationLogBizService) {
        this.adminOperationLogBizService = adminOperationLogBizService;
    }

    @Operation(summary = "操作日志列表")
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) String module,
                          @RequestParam(required = false) String action,
                          @RequestParam(required = false) Integer success,
                          @RequestParam(required = false) Long operatorId,
                          @RequestParam(defaultValue = "50") Integer limit) {
        return Result.ok(adminOperationLogBizService.list(module, action, success, operatorId, limit));
    }
}

