package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.web.admin.constant.AdminErrorCode;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminPermission;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.custom.aop.OperationLog;
import com.huang.web.admin.dto.coach.CoachScheduleUpsertDTO;
import com.huang.web.admin.service.biz.AdminCoachScheduleBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Admin Coach Schedule", description = "Coach schedule management")
@RestController
@RequestMapping("/admin/coach-schedule")
@RequireAdminRole({AdminRoleCode.ADMIN, AdminRoleCode.OPS_ADMIN})
public class AdminCoachScheduleController {

    private final AdminCoachScheduleBizService adminCoachScheduleBizService;

    public AdminCoachScheduleController(AdminCoachScheduleBizService adminCoachScheduleBizService) {
        this.adminCoachScheduleBizService = adminCoachScheduleBizService;
    }

    @Operation(summary = "Coach schedule list")
    @GetMapping("/list")
    @RequireAdminPermission({"booking:schedule"})
    public Result<?> list(@RequestParam(required = false) Long coachId,
                          @RequestParam(required = false) LocalDate scheduleDate,
                          @RequestParam(required = false) Integer status) {
        return Result.ok(adminCoachScheduleBizService.listSchedules(coachId, scheduleDate, status));
    }

    @Operation(summary = "Coach options for schedule management")
    @GetMapping("/coach-options")
    @RequireAdminPermission({"booking:schedule"})
    public Result<?> coachOptions() {
        return Result.ok(adminCoachScheduleBizService.listCoachOptions());
    }

    @Operation(summary = "Create coach schedule")
    @PostMapping
    @RequireAdminPermission({"booking:schedule"})
    @OperationLog(module = "coach_schedule", action = "create", detail = "admin create coach schedule")
    public Result<?> create(@Valid @RequestBody CoachScheduleUpsertDTO dto) {
        return Result.ok(adminCoachScheduleBizService.createSchedule(dto));
    }

    @Operation(summary = "Update coach schedule")
    @PutMapping("/{id}")
    @RequireAdminPermission({"booking:schedule"})
    @OperationLog(module = "coach_schedule", action = "update", detail = "admin update coach schedule")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody CoachScheduleUpsertDTO dto) {
        boolean ok = adminCoachScheduleBizService.updateSchedule(id, dto);
        return ok ? Result.ok("更新成功") : Result.fail(AdminErrorCode.COACH_SCHEDULE_NOT_FOUND, "教练档期不存在");
    }

    @Operation(summary = "Update coach schedule status")
    @PutMapping("/{id}/status")
    @RequireAdminPermission({"booking:schedule"})
    @OperationLog(module = "coach_schedule", action = "update_status", detail = "admin update coach schedule status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return Result.fail(AdminErrorCode.COACH_SCHEDULE_STATUS_INVALID, "教练档期状态仅支持 0/1");
        }
        boolean ok = adminCoachScheduleBizService.updateScheduleStatus(id, status);
        return ok ? Result.ok("状态更新成功") : Result.fail(AdminErrorCode.COACH_SCHEDULE_NOT_FOUND, "教练档期不存在");
    }

    @Operation(summary = "Delete coach schedule")
    @DeleteMapping("/{id}")
    @RequireAdminPermission({"booking:schedule"})
    @OperationLog(module = "coach_schedule", action = "delete", detail = "admin delete coach schedule")
    public Result<?> delete(@PathVariable Long id) {
        boolean ok = adminCoachScheduleBizService.deleteSchedule(id);
        return ok ? Result.ok("删除成功") : Result.fail(AdminErrorCode.COACH_SCHEDULE_NOT_FOUND, "教练档期不存在");
    }
}
