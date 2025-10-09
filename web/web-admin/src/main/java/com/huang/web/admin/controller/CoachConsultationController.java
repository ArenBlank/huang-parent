package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.common.result.Result;
import com.huang.web.admin.service.CoachConsultationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 教练咨询记录管理Controller
 * @author system
 * @since 2025-01-24
 */
@Tag(name = "教练咨询记录管理", description = "教练咨询记录管理相关接口")
@RestController
@RequestMapping("/admin/coach-consultation")
@Slf4j
public class CoachConsultationController {

    @Autowired
    private CoachConsultationService coachConsultationService;

    @Operation(summary = "分页查询咨询记录", description = "获取咨询记录列表（带分页和筛选）")
    @GetMapping("/page")
    public Result<Page<Map<String, Object>>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") long size,
            @Parameter(description = "教练ID") @RequestParam(required = false) Long coachId,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "咨询类型") @RequestParam(required = false) String consultationType,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "教练姓名") @RequestParam(required = false) String coachName,
            @Parameter(description = "用户昵称") @RequestParam(required = false) String userNickname,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        
        Map<String, Object> query = new HashMap<>();
        query.put("coachId", coachId);
        query.put("userId", userId);
        query.put("consultationType", consultationType);
        query.put("status", status);
        query.put("coachName", coachName);
        query.put("userNickname", userNickname);
        query.put("startDate", startDate);
        query.put("endDate", endDate);
        
        Page<Map<String, Object>> result = coachConsultationService.getConsultationPage(query, current, size);
        return Result.success(result);
    }

    @Operation(summary = "获取咨询详情", description = "根据ID获取咨询记录详细信息")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Map<String, Object> detail = coachConsultationService.getConsultationDetail(id);
        return Result.success(detail);
    }

    @Operation(summary = "更新咨询状态", description = "更新咨询记录的状态")
    @PutMapping("/{id}/status")
    public Result<String> updateStatus(
            @PathVariable Long id,
            @Parameter(description = "状态值") @RequestParam String status) {
        
        boolean success = coachConsultationService.updateConsultationStatus(id, status);
        return success ? Result.success("状态更新成功") : Result.fail("状态更新失败");
    }

    @Operation(summary = "添加教练建议", description = "为咨询记录添加教练建议")
    @PutMapping("/{id}/advice")
    public Result<String> addAdvice(
            @PathVariable Long id,
            @Parameter(description = "教练建议") @RequestParam String coachAdvice) {
        
        boolean success = coachConsultationService.addCoachAdvice(id, coachAdvice);
        return success ? Result.success("建议添加成功") : Result.fail("建议添加失败");
    }

    @Operation(summary = "获取教练咨询统计", description = "获取指定教练的咨询统计信息")
    @GetMapping("/coach/{coachId}/stats")
    public Result<Map<String, Object>> getCoachStats(@PathVariable Long coachId) {
        Map<String, Object> stats = coachConsultationService.getCoachConsultationStats(coachId);
        return Result.success(stats);
    }

    @Operation(summary = "获取用户咨询历史", description = "获取指定用户的咨询历史记录")
    @GetMapping("/user/{userId}/history")
    public Result<List<Map<String, Object>>> getUserHistory(@PathVariable Long userId) {
        List<Map<String, Object>> history = coachConsultationService.getUserConsultationHistory(userId);
        return Result.success(history);
    }

    @Operation(summary = "批量删除咨询记录", description = "批量删除咨询记录（逻辑删除）")
    @DeleteMapping("/batch")
    public Result<String> deleteBatch(@RequestBody List<Long> ids) {
        boolean success = coachConsultationService.deleteBatch(ids);
        return success ? Result.success("删除成功") : Result.fail("删除失败");
    }

    @Operation(summary = "获取咨询统计数据", description = "获取整体咨询统计数据")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = coachConsultationService.getConsultationStatistics();
        return Result.success(stats);
    }
}