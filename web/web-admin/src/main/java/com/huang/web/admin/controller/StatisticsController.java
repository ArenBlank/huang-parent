package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.web.admin.service.StatisticsService;
import com.huang.web.admin.vo.statistics.AccountStatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 统计管理Controller
 */
@Tag(name = "统计管理")
@RestController
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "获取账号统计信息")
    @GetMapping("/account")
    public Result<AccountStatisticsVO> getAccountStatistics() {
        AccountStatisticsVO statistics = statisticsService.getAccountStatistics();
        return Result.ok(statistics);
    }

    @Operation(summary = "手动执行课程统计任务")
    @PostMapping("/task/course")
    public Result<String> executeCourseStatisticsTask() {
        statisticsService.executeCourseStatisticsTask();
        return Result.ok("课程统计任务执行成功");
    }

    @Operation(summary = "手动执行教练统计任务")
    @PostMapping("/task/coach")
    public Result<String> executeCoachStatisticsTask() {
        statisticsService.executeCoachStatisticsTask();
        return Result.ok("教练统计任务执行成功");
    }

    @Operation(summary = "手动执行课程状态检查任务")
    @PostMapping("/task/course-status")
    public Result<String> executeCourseStatusCheckTask() {
        statisticsService.executeCourseStatusCheckTask();
        return Result.ok("课程状态检查任务执行成功");
    }

    @Operation(summary = "手动执行临时文件清理任务")
    @PostMapping("/task/clean-temp-files")
    public Result<String> cleanExpiredTempFiles() {
        statisticsService.cleanExpiredTempFiles();
        return Result.ok("临时文件清理任务执行成功");
    }
}
