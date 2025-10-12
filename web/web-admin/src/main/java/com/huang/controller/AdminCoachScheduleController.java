package com.huang.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.huang.common.result.Result;
import com.huang.dto.AdminCoachScheduleQueryDTO;
import com.huang.dto.CoachScheduleReviewDTO;
import com.huang.service.AdminCoachScheduleChangeService;
import com.huang.vo.AdminCoachScheduleChangeDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Admin端教练日程审核Controller
 * @author huang
 * @since 2025-01-24
 */
@Tag(name = "Admin端教练日程审核", description = "教练日程变更审核相关接口")
@RestController
@RequestMapping("/admin/coach/schedule")
@RequiredArgsConstructor
@Slf4j
public class AdminCoachScheduleController {

    private final AdminCoachScheduleChangeService scheduleChangeService;

    @Operation(summary = "分页查询日程变更申请", description = "分页查询所有教练的日程变更申请记录")
    @GetMapping("/change/page")
    public Result<IPage<AdminCoachScheduleChangeDetailVO>> getCoachScheduleChangePage(
            @Parameter(description = "查询条件") @ModelAttribute AdminCoachScheduleQueryDTO query) {
        IPage<AdminCoachScheduleChangeDetailVO> page = scheduleChangeService.getCoachScheduleChangePageForAdmin(query);
        return Result.success(page);
    }

    @Operation(summary = "查询日程变更申请详情", description = "根据ID查询日程变更申请详情")
    @GetMapping("/change/{id}")
    public Result<AdminCoachScheduleChangeDetailVO> getCoachScheduleChangeDetail(
            @Parameter(description = "申请ID", required = true) @PathVariable Long id) {
        AdminCoachScheduleChangeDetailVO detail = scheduleChangeService.getCoachScheduleChangeDetailForAdmin(id);
        return Result.success(detail);
    }

    @Operation(summary = "审核日程变更申请", description = "审核教练的日程变更申请")
    @PostMapping("/change/review")
    public Result<Void> reviewCoachScheduleChange(
            @Parameter(description = "JWT令牌", required = true) @RequestHeader("Authorization") String token,
            @Parameter(description = "审核信息", required = true) @Valid @RequestBody CoachScheduleReviewDTO dto) {
        scheduleChangeService.reviewCoachScheduleChange(token, dto);
        return Result.success();
    }
}