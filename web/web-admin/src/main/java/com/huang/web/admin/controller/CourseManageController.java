package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.web.admin.constant.AdminErrorCode;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.custom.aop.OperationLog;
import com.huang.web.admin.dto.course.CourseScheduleCreateDTO;
import com.huang.web.admin.dto.course.CourseUpsertDTO;
import com.huang.web.admin.service.biz.AdminCourseOpsBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin课程运营", description = "课程与排期运营管理")
@RestController
@RequestMapping("/admin/course")
@RequireAdminRole({AdminRoleCode.ADMIN, AdminRoleCode.OPS_ADMIN})
public class CourseManageController {

    private final AdminCourseOpsBizService adminCourseOpsBizService;

    public CourseManageController(AdminCourseOpsBizService adminCourseOpsBizService) {
        this.adminCourseOpsBizService = adminCourseOpsBizService;
    }

    @Operation(summary = "课程列表")
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) Integer status,
                          @RequestParam(required = false) Long categoryId) {
        return Result.ok(adminCourseOpsBizService.listCourses(status, categoryId));
    }

    @Operation(summary = "新增课程")
    @PostMapping
    @OperationLog(module = "course", action = "create", detail = "admin create course")
    public Result<?> create(@Valid @RequestBody CourseUpsertDTO dto) {
        return Result.ok(adminCourseOpsBizService.createCourse(dto));
    }

    @Operation(summary = "更新课程")
    @PutMapping("/{id}")
    @OperationLog(module = "course", action = "update", detail = "admin update course")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody CourseUpsertDTO dto) {
        boolean ok = adminCourseOpsBizService.updateCourse(id, dto);
        return ok ? Result.ok("更新成功") : Result.fail(AdminErrorCode.COURSE_NOT_FOUND, "课程不存在");
    }

    @Operation(summary = "更新课程状态")
    @PutMapping("/{id}/status")
    @OperationLog(module = "course", action = "publish", detail = "admin update course status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return Result.fail(AdminErrorCode.COURSE_STATUS_INVALID, "课程状态仅支持 0/1");
        }
        boolean ok = adminCourseOpsBizService.updateCourseStatus(id, status);
        return ok ? Result.ok("状态更新成功") : Result.fail(AdminErrorCode.COURSE_NOT_FOUND, "课程不存在");
    }

    @Operation(summary = "排期列表")
    @GetMapping("/schedule/list")
    public Result<?> scheduleList(@RequestParam(required = false) Long courseId,
                                  @RequestParam(required = false) Integer status) {
        return Result.ok(adminCourseOpsBizService.listSchedules(courseId, status));
    }

    @Operation(summary = "新增排期")
    @PostMapping("/schedule")
    @OperationLog(module = "course_schedule", action = "create", detail = "admin create course schedule")
    public Result<?> createSchedule(@Valid @RequestBody CourseScheduleCreateDTO dto) {
        Long id = adminCourseOpsBizService.createSchedule(dto);
        return id == null ? Result.fail(AdminErrorCode.COURSE_NOT_FOUND, "课程不存在") : Result.ok(id);
    }

    @Operation(summary = "更新排期状态")
    @PutMapping("/schedule/{id}/status")
    @OperationLog(module = "course_schedule", action = "update_status", detail = "admin update course schedule status")
    public Result<?> updateScheduleStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return Result.fail(AdminErrorCode.COURSE_SCHEDULE_STATUS_INVALID, "排期状态仅支持 0/1");
        }
        boolean ok = adminCourseOpsBizService.updateScheduleStatus(id, status);
        return ok ? Result.ok("状态更新成功") : Result.fail(AdminErrorCode.COURSE_SCHEDULE_NOT_FOUND, "排期不存在");
    }
}
