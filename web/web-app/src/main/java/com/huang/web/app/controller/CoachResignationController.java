package com.huang.web.app.controller;

import com.huang.common.result.Result;
import com.huang.web.app.dto.coach.CoachResignationApplyDTO;
import com.huang.web.app.service.CoachResignationService;
import com.huang.web.app.vo.coach.CoachResignationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * App端教练离职申请Controller
 * @author system
 * @since 2025-01-25
 */
@Tag(name = "教练离职管理", description = "教练离职申请相关接口")
@RestController
@RequestMapping("/app/coach/resignation")
@Slf4j
@Validated
public class CoachResignationController {

    @Autowired
    private CoachResignationService coachResignationService;

    /**
     * 提交教练离职申请
     */
    @Operation(summary = "提交教练离职申请", description = "教练提交离职申请,需提供预计离职日期、离职原因等信息")
    @PostMapping("/apply")
    public Result<Long> applyResignation(@RequestBody @Valid CoachResignationApplyDTO applyDTO) {
        try {
            log.info("提交教练离职申请: {}", applyDTO);
            Long applicationId = coachResignationService.applyResignation(applyDTO);
            return Result.ok(applicationId);
        } catch (RuntimeException e) {
            log.error("提交教练离职申请失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("提交教练离职申请异常", e);
            return Result.fail("提交教练离职申请失败");
        }
    }

    /**
     * 获取当前教练的离职申请状态
     */
    @Operation(summary = "获取当前教练的离职申请状态", description = "查询当前教练是否有待审核的离职申请")
    @GetMapping("/current-status")
    public Result<CoachResignationVO> getCurrentResignationStatus() {
        try {
            log.info("获取当前教练的离职申请状态");
            CoachResignationVO status = coachResignationService.getCurrentResignationStatus();
            return Result.ok(status);
        } catch (RuntimeException e) {
            log.error("获取离职申请状态失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("获取离职申请状态异常", e);
            return Result.fail("获取离职申请状态失败");
        }
    }

    /**
     * 获取离职申请历史记录
     */
    @Operation(summary = "获取离职申请历史记录", description = "查询教练的所有离职申请记录")
    @GetMapping("/history")
    public Result<List<CoachResignationVO>> getResignationHistory() {
        try {
            log.info("获取离职申请历史记录");
            List<CoachResignationVO> history = coachResignationService.getResignationHistory();
            return Result.ok(history);
        } catch (RuntimeException e) {
            log.error("获取离职申请历史失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("获取离职申请历史异常", e);
            return Result.fail("获取离职申请历史失败");
        }
    }

    /**
     * 获取离职申请详情
     */
    @Operation(summary = "获取离职申请详情", description = "根据申请ID查询离职申请的详细信息")
    @GetMapping("/{applicationId}")
    public Result<CoachResignationVO> getResignationDetail(
            @Parameter(description = "申请ID", required = true)
            @PathVariable @NotNull @Min(1) Long applicationId) {
        try {
            log.info("获取离职申请详情, 申请ID: {}", applicationId);
            CoachResignationVO detail = coachResignationService.getResignationDetail(applicationId);
            return Result.ok(detail);
        } catch (RuntimeException e) {
            log.error("获取离职申请详情失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("获取离职申请详情异常", e);
            return Result.fail("获取离职申请详情失败");
        }
    }

    /**
     * 撤销离职申请
     */
    @Operation(summary = "撤销离职申请", description = "教练撤销自己的待审核离职申请")
    @DeleteMapping("/{applicationId}/cancel")
    public Result<Void> cancelResignation(
            @Parameter(description = "申请ID", required = true)
            @PathVariable @NotNull @Min(1) Long applicationId) {
        try {
            log.info("撤销离职申请, 申请ID: {}", applicationId);
            boolean success = coachResignationService.cancelResignation(applicationId);
            if (success) {
                return Result.ok();
            } else {
                return Result.fail("撤销离职申请失败");
            }
        } catch (RuntimeException e) {
            log.error("撤销离职申请失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("撤销离职申请异常", e);
            return Result.fail("撤销离职申请失败");
        }
    }
}
