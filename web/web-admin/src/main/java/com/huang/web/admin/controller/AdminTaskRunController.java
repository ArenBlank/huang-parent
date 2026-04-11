package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.common.result.Result;
import com.huang.model.entity.TaskRunLog;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminPermission;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.custom.aop.OperationLog;
import com.huang.web.admin.service.biz.AdminTaskRunBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Admin Task Run", description = "Scheduled task run logs and manual trigger")
@RestController
@RequestMapping("/admin/task-run")
@RequireAdminRole({AdminRoleCode.ADMIN, AdminRoleCode.OPS_ADMIN, AdminRoleCode.AUDIT_ADMIN})
public class AdminTaskRunController {

    private final AdminTaskRunBizService adminTaskRunBizService;

    public AdminTaskRunController(AdminTaskRunBizService adminTaskRunBizService) {
        this.adminTaskRunBizService = adminTaskRunBizService;
    }

    @Operation(summary = "Task run page")
    @GetMapping("/page")
    @RequireAdminPermission({"task:run:read"})
    public Result<Page<TaskRunLog>> page(@RequestParam(required = false) String taskCode,
                                         @RequestParam(required = false) String runStatus,
                                         @RequestParam(required = false) String triggerMode,
                                         @RequestParam(defaultValue = "1") Integer pageNo,
                                         @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(adminTaskRunBizService.page(taskCode, runStatus, triggerMode, pageNo, pageSize));
    }

    @Operation(summary = "Task run summary")
    @GetMapping("/summary")
    @RequireAdminPermission({"task:run:read"})
    public Result<Map<String, Object>> summary() {
        return Result.ok(adminTaskRunBizService.summary());
    }

    @Operation(summary = "Trigger task manually")
    @PostMapping("/{taskCode}/trigger")
    @RequireAdminPermission({"task:run:trigger"})
    @OperationLog(module = "task_run", action = "trigger", detail = "admin trigger task manually")
    public Result<TaskRunLog> trigger(@PathVariable String taskCode) {
        return Result.ok(adminTaskRunBizService.trigger(taskCode));
    }
}
