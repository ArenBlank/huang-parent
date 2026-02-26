package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.web.admin.service.biz.AdminOpsBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin看板", description = "运营看板数据")
@RestController
@RequestMapping("/admin/dashboard")
public class DashboardController {

    private final AdminOpsBizService adminOpsBizService;

    public DashboardController(AdminOpsBizService adminOpsBizService) {
        this.adminOpsBizService = adminOpsBizService;
    }

    @Operation(summary = "看板汇总")
    @GetMapping("/summary")
    public Result<?> summary() {
        return Result.ok(adminOpsBizService.dashboardSummary());
    }
}

