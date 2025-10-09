package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.huang.common.result.Result;
import com.huang.web.admin.dto.coach.CoachResignationQueryDTO;
import com.huang.web.admin.dto.coach.CoachResignationReviewDTO;
import com.huang.web.admin.service.CoachResignationService;
import com.huang.web.admin.vo.coach.CoachResignationDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Admin端教练离职申请Controller
 * @author system
 * @since 2025-01-24
 */
@Tag(name = "教练离职申请管理", description = "教练离职申请管理相关接口")
@RestController
@RequestMapping("/admin/coach/resignation")
@Slf4j
@Validated
public class CoachResignationController {

    @Autowired
    private CoachResignationService coachResignationService;

    @Operation(summary = "获取教练离职申请列表", description = "获取教练离职申请列表")
    @GetMapping("/list")
    public Result<IPage<CoachResignationDetailVO>> getResignationApplications(@Valid CoachResignationQueryDTO queryDTO) {
        try {
            log.info("查询教练离职申请列表: {}", queryDTO);
            IPage<CoachResignationDetailVO> page = coachResignationService.getResignationApplications(queryDTO);
            return Result.success(page);
        } catch (Exception e) {
            log.error("获取教练离职申请列表异常: {}", e.getMessage(), e);
            return Result.failed("获取教练离职申请列表失败");
        }
    }

    @Operation(summary = "查看教练离职申请详情", description = "查看教练离职申请详情")
    @GetMapping("/{id}")
    public Result<CoachResignationDetailVO> getResignationDetail(
            @Parameter(description = "申请ID", required = true) @PathVariable Long id) {
        try {
            log.info("查看教练离职申请详情: id={}", id);
            CoachResignationDetailVO detail = coachResignationService.getResignationDetail(id);
            if (detail == null) {
                return Result.failed("离职申请不存在");
            }
            return Result.success(detail);
        } catch (Exception e) {
            log.error("获取离职申请详情异常: {}", e.getMessage(), e);
            return Result.failed("获取离职申请详情失败");
        }
    }

    @Operation(summary = "批准教练离职申请", description = "批准教练离职申请")
    @PostMapping("/approve/{id}")
    public Result<Boolean> approveResignation(
            @Parameter(description = "申请ID", required = true) @PathVariable Long id,
            @RequestBody @Valid CoachResignationReviewDTO reviewDTO) {
        try {
            log.info("批准教练离职申请: id={}, reviewDTO={}", id, reviewDTO);
            boolean success = coachResignationService.approveResignation(id, reviewDTO);
            if (success) {
                log.info("批准教练离职申请成功: id={}", id);
                return Result.success(true);
            } else {
                log.error("批准教练离职申请失败: id={}", id);
                return Result.failed("离职申请批准失败");
            }
        } catch (Exception e) {
            log.error("批准教练离职申请异常: {}", e.getMessage(), e);
            return Result.failed(e.getMessage());
        }
    }

    @Operation(summary = "拒绝教练离职申请", description = "拒绝教练离职申请")
    @PostMapping("/reject/{id}")
    public Result<Boolean> rejectResignation(
            @Parameter(description = "申请ID", required = true) @PathVariable Long id,
            @RequestBody @Valid CoachResignationReviewDTO reviewDTO) {
        try {
            log.info("拒绝教练离职申请: id={}, reviewDTO={}", id, reviewDTO);
            boolean success = coachResignationService.rejectResignation(id, reviewDTO);
            if (success) {
                log.info("拒绝教练离职申请成功: id={}", id);
                return Result.success(true);
            } else {
                log.error("拒绝教练离职申请失败: id={}", id);
                return Result.failed("离职申请拒绝失败");
            }
        } catch (Exception e) {
            log.error("拒绝教练离职申请异常: {}", e.getMessage(), e);
            return Result.failed(e.getMessage());
        }
    }

    @Operation(summary = "取消教练离职申请", description = "取消教练离职申请")
    @DeleteMapping("/cancel/{id}")
    public Result<Boolean> cancelResignation(
            @Parameter(description = "申请ID", required = true) @PathVariable Long id,
            @RequestParam(required = false) String cancelReason) {
        try {
            log.info("取消教练离职申请: id={}, cancelReason={}", id, cancelReason);
            boolean success = coachResignationService.cancelResignation(id, cancelReason);
            if (success) {
                log.info("取消教练离职申请成功: id={}", id);
                return Result.success(true);
            } else {
                log.error("取消教练离职申请失败: id={}", id);
                return Result.failed("离职申请取消失败");
            }
        } catch (Exception e) {
            log.error("取消教练离职申请异常: {}", e.getMessage(), e);
            return Result.failed(e.getMessage());
        }
    }
}