package com.huang.web.app.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.huang.common.result.Result;
import com.huang.web.app.dto.CoachAvailabilityDTO;
import com.huang.web.app.dto.CoachScheduleChangeDTO;
import com.huang.web.app.dto.CoachScheduleQueryDTO;
import com.huang.web.app.service.AppCoachAvailabilityService;
import com.huang.web.app.service.AppCoachScheduleChangeService;
import com.huang.web.app.vo.coach.CoachAvailabilityVO;
import com.huang.web.app.vo.coach.CoachScheduleChangeDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * App端教练日程管理Controller
 * @author huang
 * @since 2025-01-24
 */
@Tag(name = "App端教练日程管理", description = "教练日程管理相关接口")
@RestController
@RequestMapping("/app/coach/schedule")
@RequiredArgsConstructor
@Slf4j
public class AppCoachScheduleController {

    private final AppCoachScheduleChangeService scheduleChangeService;
    private final AppCoachAvailabilityService availabilityService;

    @Operation(summary = "申请请假/加班/调整", description = "教练申请日程变更（请假、加班、调整时间）")
    @PostMapping("/change/apply")
    public Result<Void> applyScheduleChange(
            @Parameter(description = "JWT令牌", required = true) @RequestHeader("Authorization") String token,
            @Parameter(description = "日程变更申请信息", required = true) @Valid @RequestBody CoachScheduleChangeDTO dto) {
        scheduleChangeService.applyScheduleChange(token, dto);
        return Result.success();
    }

    @Operation(summary = "查看我的请假记录", description = "分页查询当前教练的所有日程变更申请记录")
    @GetMapping("/change/page")
    public Result<IPage<CoachScheduleChangeDetailVO>> getScheduleChangePage(
            @Parameter(description = "JWT令牌", required = true) @RequestHeader("Authorization") String token,
            @Parameter(description = "查询条件") @ModelAttribute CoachScheduleQueryDTO query) {
        IPage<CoachScheduleChangeDetailVO> page = scheduleChangeService.getScheduleChangePage(token, query);
        return Result.success(page);
    }

    @Operation(summary = "查看申请详情", description = "根据ID查询日程变更申请的详细信息")
    @GetMapping("/change/{id}")
    public Result<CoachScheduleChangeDetailVO> getScheduleChangeDetail(
            @Parameter(description = "JWT令牌", required = true) @RequestHeader("Authorization") String token,
            @Parameter(description = "申请ID", required = true) @PathVariable Long id) {
        CoachScheduleChangeDetailVO detail = scheduleChangeService.getScheduleChangeDetail(token, id);
        return Result.success(detail);
    }

    @Operation(summary = "撤销申请", description = "撤销待审核状态的日程变更申请")
    @DeleteMapping("/change/{id}")
    public Result<Void> cancelScheduleChange(
            @Parameter(description = "JWT令牌", required = true) @RequestHeader("Authorization") String token,
            @Parameter(description = "申请ID", required = true) @PathVariable Long id) {
        scheduleChangeService.cancelScheduleChange(token, id);
        return Result.success();
    }

    @Operation(summary = "查看我的工作时间", description = "查看当前教练的工作时间安排（一周7天的时间设置）")
    @GetMapping("/availability/list")
    public Result<List<CoachAvailabilityVO>> getCoachAvailabilityList(
            @Parameter(description = "JWT令牌", required = true) @RequestHeader("Authorization") String token) {
        List<CoachAvailabilityVO> list = availabilityService.getCoachAvailabilityList(token);
        return Result.success(list);
    }

    @Operation(summary = "设置工作时间", description = "设置教练在某个星期几的工作时间（如周一9:00-18:00）")
    @PostMapping("/availability/set")
    public Result<Void> setCoachAvailability(
            @Parameter(description = "JWT令牌", required = true) @RequestHeader("Authorization") String token,
            @Parameter(description = "工作时间设置信息", required = true) @Valid @RequestBody CoachAvailabilityDTO dto) {
        availabilityService.setCoachAvailability(token, dto);
        return Result.success();
    }

    @Operation(summary = "删除工作时间", description = "删除教练的某个工作时间设置")
    @DeleteMapping("/availability/{id}")
    public Result<Void> deleteCoachAvailability(
            @Parameter(description = "JWT令牌", required = true) @RequestHeader("Authorization") String token,
            @Parameter(description = "工作时间设置ID", required = true) @PathVariable Long id) {
        availabilityService.deleteCoachAvailability(token, id);
        return Result.success();
    }
}
