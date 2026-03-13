package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.web.admin.constant.AdminErrorCode;
import com.huang.web.admin.constant.AdminRoleCode;
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

@Tag(name = "Admin预约订单", description = "预约与订单管�?)
@RestController
@RequestMapping("/admin/ops")
@RequireAdminRole({AdminRoleCode.ADMIN, AdminRoleCode.OPS_ADMIN})
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

    @Operation(summary = "管理员完成授�?)
    @PostMapping("/booking/complete")
    @OperationLog(module = "booking", action = "complete", detail = "admin complete booking")
    public Result<?> complete(@RequestParam Long bookingId) {
        boolean ok = adminOpsBizService.markBookingCompleted(bookingId);
        return ok ? Result.ok("处理成功") : Result.fail(AdminErrorCode.BOOKING_COMPLETE_FAILED, "处理失败：当前状态不允许完成");
    }

    @Operation(summary = "订单列表")
    @GetMapping("/order/list")
    public Result<?> orderList(@RequestParam(required = false) String payStatus) {
        return Result.ok(adminOpsBizService.orderList(payStatus));
    }
    @Operation(summary = "订单详情(含明细条)")
    @GetMapping("/order/detail")
    public Result<?> orderDetail(@RequestParam Long orderId) {
        var detail = adminOpsBizService.orderDetail(orderId);
        return detail == null ? Result.fail(AdminErrorCode.ORDER_NOT_FOUND, "order not found") : Result.ok(detail);
    }

    @Operation(summary = "手动关闭超时未支付预�?)
    @PostMapping("/booking/close-timeout")
    @OperationLog(module = "booking", action = "close_timeout", detail = "admin close timeout bookings")
    public Result<?> closeTimeout(@RequestParam(defaultValue = "30") Integer timeoutMinutes) {
        return Result.ok(adminOpsBizService.closeTimeoutUnpaidBookings(timeoutMinutes));
    }
}



