package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.web.admin.constant.AdminErrorCode;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminPermission;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.custom.aop.OperationLog;
import com.huang.web.admin.service.biz.AdminOpsBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Booking", description = "Booking and order ops")
@RestController
@RequestMapping("/admin/ops")
@RequireAdminRole({AdminRoleCode.ADMIN, AdminRoleCode.OPS_ADMIN})
public class BookingManageController {

    private final AdminOpsBizService adminOpsBizService;

    public BookingManageController(AdminOpsBizService adminOpsBizService) {
        this.adminOpsBizService = adminOpsBizService;
    }

    @Operation(summary = "Booking list")
    @GetMapping("/booking/list")
    @RequireAdminPermission({"booking:read"})
    public Result<?> bookingList(@RequestParam(required = false) String status) {
        return Result.ok(adminOpsBizService.bookingList(status));
    }

    @Operation(summary = "Mark booking completed")
    @PostMapping("/booking/complete")
    @RequireAdminPermission({"booking:manage"})
    @OperationLog(module = "booking", action = "complete", detail = "admin complete booking")
    public Result<?> complete(@RequestParam Long bookingId) {
        boolean ok = adminOpsBizService.markBookingCompleted(bookingId);
        return ok ? Result.ok("success")
                : Result.fail(AdminErrorCode.BOOKING_COMPLETE_FAILED, "booking status invalid");
    }

    @Operation(summary = "Order list")
    @GetMapping("/order/list")
    @RequireAdminPermission({"order:read"})
    public Result<?> orderList(@RequestParam(required = false) String payStatus) {
        return Result.ok(adminOpsBizService.orderList(payStatus));
    }

    @Operation(summary = "Order detail (with items)")
    @GetMapping("/order/detail")
    @RequireAdminPermission({"order:read"})
    public Result<?> orderDetail(@RequestParam Long orderId) {
        var detail = adminOpsBizService.orderDetail(orderId);
        return detail == null ? Result.fail(AdminErrorCode.ORDER_NOT_FOUND, "order not found") : Result.ok(detail);
    }

    @Operation(summary = "Close timeout unpaid bookings")
    @PostMapping("/booking/close-timeout")
    @RequireAdminPermission({"booking:manage"})
    @OperationLog(module = "booking", action = "close_timeout", detail = "admin close timeout bookings")
    public Result<?> closeTimeout(@RequestParam(defaultValue = "30") Integer timeoutMinutes) {
        return Result.ok(adminOpsBizService.closeTimeoutUnpaidBookings(timeoutMinutes));
    }
}
