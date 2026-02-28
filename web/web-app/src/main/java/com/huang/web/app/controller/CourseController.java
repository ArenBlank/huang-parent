package com.huang.web.app.controller;

import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.common.result.Result;
import com.huang.web.app.dto.course.CourseEnrollDTO;
import com.huang.web.app.dto.course.CourseRefundDTO;
import com.huang.web.app.service.biz.CourseLearningBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "App课程学习", description = "课程浏览、报名下单、支付回调、报名记录")
@RestController
@RequestMapping("/app/course")
public class CourseController {

    private final CourseLearningBizService courseLearningBizService;

    public CourseController(CourseLearningBizService courseLearningBizService) {
        this.courseLearningBizService = courseLearningBizService;
    }

    @Operation(summary = "课程列表")
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) Long categoryId) {
        return Result.ok(courseLearningBizService.listCourses(categoryId));
    }

    @Operation(summary = "课程排期列表")
    @GetMapping("/{courseId}/schedule/list")
    public Result<?> schedules(@PathVariable Long courseId) {
        return Result.ok(courseLearningBizService.listSchedules(courseId));
    }

    @Operation(summary = "课程报名并下单")
    @PostMapping("/enroll")
    public Result<?> enroll(@Valid @RequestBody CourseEnrollDTO dto) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return Result.fail("未登录");
        }
        var result = courseLearningBizService.enroll(loginUser.getUserId(), dto);
        return result == null ? Result.fail("报名失败，排期可能已满或重复报名") : Result.ok(result);
    }

    @Operation(summary = "课程支付成功回调(模拟)")
    @PostMapping("/pay-success")
    public Result<?> paySuccess(@RequestParam Long enrollmentId) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return Result.fail("未登录");
        }
        boolean ok = courseLearningBizService.markPaySuccess(enrollmentId, loginUser.getUserId());
        return ok ? Result.ok("支付状态更新成功") : Result.fail("支付状态更新失败");
    }

    @Operation(summary = "取消未支付报名")
    @PostMapping("/cancel-unpaid")
    public Result<?> cancelUnpaid(@RequestParam Long enrollmentId) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return Result.fail("未登录");
        }
        boolean ok = courseLearningBizService.cancelUnpaid(enrollmentId, loginUser.getUserId());
        return ok ? Result.ok("已取消报名") : Result.fail("取消失败，仅支持未支付报名取消");
    }

    @Operation(summary = "发起课程退款")
    @PostMapping("/refund")
    public Result<?> refund(@Valid @RequestBody CourseRefundDTO dto) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return Result.fail("未登录");
        }
        boolean ok = courseLearningBizService.refundPaid(dto.getEnrollmentId(), loginUser.getUserId(), dto.getReason());
        return ok ? Result.ok("退款成功") : Result.fail("退款失败，仅支持已支付报名退款");
    }

    @Operation(summary = "我的课程报名记录")
    @GetMapping("/my/enrollments")
    public Result<?> myEnrollments() {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return Result.fail("未登录");
        }
        return Result.ok(courseLearningBizService.myEnrollments(loginUser.getUserId()));
    }
}
