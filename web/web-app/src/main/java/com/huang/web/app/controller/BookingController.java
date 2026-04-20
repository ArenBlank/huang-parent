package com.huang.web.app.controller;

import com.huang.common.constant.RedisConstant;
import com.huang.common.guard.IdempotentSubmit;
import com.huang.common.guard.RateLimit;
import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.common.result.Result;
import com.huang.web.app.dto.booking.BookingReviewDTO;
import com.huang.web.app.dto.booking.CreateBookingDTO;
import com.huang.web.app.service.biz.BookingBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "App预约支付", description = "教练预约与支付流程")
@RestController
@RequestMapping("/app/booking")
public class BookingController {

    private final BookingBizService bookingBizService;

    public BookingController(BookingBizService bookingBizService) {
        this.bookingBizService = bookingBizService;
    }

    @Operation(summary = "可预约档期")
    @GetMapping("/schedule/list")
    public Result<?> scheduleList(@RequestParam(required = false) Long coachId,
                                  @RequestParam(required = false) LocalDate date) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        Long userId = loginUser == null ? null : loginUser.getUserId();
        return Result.ok(bookingBizService.listSchedule(userId, coachId, date));
    }

    @Operation(summary = "档期摘要")
    @GetMapping("/schedule/summary")
    public Result<?> scheduleSummary(@RequestParam(required = false) Long coachId) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        Long userId = loginUser == null ? null : loginUser.getUserId();
        return Result.ok(bookingBizService.scheduleSummary(userId, coachId));
    }

    @Operation(summary = "创建预约订单")
    @IdempotentSubmit(
            prefix = RedisConstant.APP_BOOKING_CREATE_IDEMPOTENT_PREFIX,
            key = "#userId + ':' + #dto.scheduleId",
            ttlSec = RedisConstant.IDEMPOTENT_TTL_SEC,
            message = "请勿重复提交预约请求"
    )
    @RateLimit(
            prefix = RedisConstant.APP_BOOKING_CREATE_LIMIT_PREFIX,
            key = "#userId",
            maxRequests = RedisConstant.SENSITIVE_WRITE_RATE_LIMIT_MAX,
            windowSec = RedisConstant.SENSITIVE_WRITE_RATE_LIMIT_WINDOW_SEC,
            message = "预约请求过于频繁，请稍后再试"
    )
    @PostMapping("/create")
    public Result<?> create(@Valid @RequestBody CreateBookingDTO dto) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return Result.fail("未登录");
        }
        Long userId = loginUser.getUserId();
        var result = bookingBizService.createBooking(userId, dto);
        return result == null
                ? Result.fail("预约失败，档期可能已满或已预约")
                : Result.ok(result);
    }

    @Operation(summary = "支付成功回调(模拟)")
    @PostMapping("/pay-success")
    public Result<?> paySuccess(@RequestParam Long bookingId) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return Result.fail("未登录");
        }
        boolean ok = bookingBizService.markPaySuccess(bookingId, loginUser.getUserId());
        return ok ? Result.ok("支付状态更新成功") : Result.fail("支付状态更新失败");
    }

    @Operation(summary = "确认授课完成")
    @PostMapping("/complete")
    public Result<?> complete(@RequestParam Long bookingId) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return Result.fail("未登录");
        }
        boolean ok = bookingBizService.completeBooking(bookingId, loginUser.getUserId());
        return ok ? Result.ok("预约已完成") : Result.fail("当前状态不可完成");
    }

    @Operation(summary = "提交评价")
    @PostMapping("/review")
    public Result<?> review(@Valid @RequestBody BookingReviewDTO dto) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return Result.fail("未登录");
        }
        boolean ok = bookingBizService.review(loginUser.getUserId(), dto);
        return ok ? Result.ok("评价成功") : Result.fail("评价失败，仅完成后可评价且仅可评价一次");
    }

    @Operation(summary = "我的预约列表")
    @GetMapping("/my/list")
    public Result<?> myList() {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return Result.fail("未登录");
        }
        return Result.ok(bookingBizService.myBookings(loginUser.getUserId()));
    }
}
