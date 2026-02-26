package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.web.admin.service.biz.AdminOpsBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin预约订单", description = "预约与订单管理")
@RestController
@RequestMapping("/admin/ops")
public class BookingManageController {

    private final AdminOpsBizService adminOpsBizService;

    public BookingManageController(AdminOpsBizService adminOpsBizService) {
        this.adminOpsBizService = adminOpsBizService;
    }

    @Operation(summary = "预约列表")
    @GetMapping("/booking/list")
    public Result<?> bookingList(@RequestParam(required = false) String status) {
        return Result.ok(adminOpsBizService.bookingList(status));
    }

    @Operation(summary = "管理员完成授课")
    @PostMapping("/booking/complete")
    public Result<?> complete(@RequestParam Long bookingId) {
        boolean ok = adminOpsBizService.markBookingCompleted(bookingId);
        return ok ? Result.ok("处理成功") : Result.fail("处理失败，当前状态不允许");
    }

    @Operation(summary = "订单列表")
    @GetMapping("/order/list")
    public Result<?> orderList(@RequestParam(required = false) String payStatus) {
        return Result.ok(adminOpsBizService.orderList(payStatus));
    }

    @Operation(summary = "手动关闭超时未支付预约")
    @PostMapping("/booking/close-timeout")
    public Result<?> closeTimeout(@RequestParam(defaultValue = "30") Integer timeoutMinutes) {
        return Result.ok(adminOpsBizService.closeTimeoutUnpaidBookings(timeoutMinutes));
    }
}
