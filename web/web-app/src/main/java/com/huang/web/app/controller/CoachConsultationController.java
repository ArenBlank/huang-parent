package com.huang.web.app.controller;

import com.huang.common.result.Result;
import com.huang.web.app.dto.coach.CoachConsultationBookDTO;
import com.huang.web.app.service.CoachConsultationService;
import com.huang.web.app.vo.coach.CoachConsultationVO;
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
 * App端教练咨询Controller
 * @author system
 * @since 2025-01-25
 */
@Tag(name = "教练咨询", description = "教练咨询预约相关接口")
@RestController
@RequestMapping("/app/consultation")
@Slf4j
@Validated
public class CoachConsultationController {

    @Autowired
    private CoachConsultationService coachConsultationService;

    /**
     * 预约教练咨询
     */
    @Operation(summary = "预约教练咨询", description = "用户预约教练进行在线或线下咨询")
    @PostMapping("/book")
    public Result<Long> bookConsultation(@RequestBody @Valid CoachConsultationBookDTO bookDTO) {
        try {
            log.info("预约教练咨询: {}", bookDTO);
            Long consultationId = coachConsultationService.bookConsultation(bookDTO);
            return Result.ok(consultationId);
        } catch (RuntimeException e) {
            log.error("预约教练咨询失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("预约教练咨询异常", e);
            return Result.fail("预约教练咨询失败");
        }
    }

    /**
     * 查看我的咨询记录
     */
    @Operation(summary = "我的咨询记录", description = "查询当前用户的所有咨询记录，可按状态筛选")
    @GetMapping("/my-list")
    public Result<List<CoachConsultationVO>> getMyConsultations(
            @Parameter(description = "状态筛选: scheduled已预约 | completed已完成 | cancelled已取消")
            @RequestParam(required = false) String status) {
        try {
            log.info("查询我的咨询记录, 状态筛选: {}", status);
            List<CoachConsultationVO> consultations = coachConsultationService.getMyConsultations(status);
            return Result.ok(consultations);
        } catch (RuntimeException e) {
            log.error("查询咨询记录失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询咨询记录异常", e);
            return Result.fail("查询咨询记录失败");
        }
    }

    /**
     * 获取咨询详情
     */
    @Operation(summary = "查看咨询详情", description = "根据咨询ID查看咨询的详细信息")
    @GetMapping("/{id}")
    public Result<CoachConsultationVO> getConsultationDetail(
            @Parameter(description = "咨询ID", required = true)
            @PathVariable @NotNull @Min(1) Long id) {
        try {
            log.info("获取咨询详情, 咨询ID: {}", id);
            CoachConsultationVO detail = coachConsultationService.getConsultationDetail(id);
            return Result.ok(detail);
        } catch (RuntimeException e) {
            log.error("获取咨询详情失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("获取咨询详情异常", e);
            return Result.fail("获取咨询详情失败");
        }
    }

    /**
     * 取消咨询
     */
    @Operation(summary = "取消预约", description = "取消已预约的咨询（只有未开始的咨询可以取消）")
    @DeleteMapping("/{id}/cancel")
    public Result<Void> cancelConsultation(
            @Parameter(description = "咨询ID", required = true)
            @PathVariable @NotNull @Min(1) Long id) {
        try {
            log.info("取消咨询, 咨询ID: {}", id);
            boolean success = coachConsultationService.cancelConsultation(id);
            if (success) {
                return Result.ok();
            } else {
                return Result.fail("取消咨询失败");
            }
        } catch (RuntimeException e) {
            log.error("取消咨询失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("取消咨询异常", e);
            return Result.fail("取消咨询失败");
        }
    }
}
