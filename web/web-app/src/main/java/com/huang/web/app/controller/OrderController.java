package com.huang.web.app.controller;

import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.common.result.Result;
import com.huang.web.app.service.biz.OrderBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "App Order", description = "Order detail query")
@RestController
@RequestMapping("/app/order")
public class OrderController {

    private final OrderBizService orderBizService;

    public OrderController(OrderBizService orderBizService) {
        this.orderBizService = orderBizService;
    }

    @Operation(summary = "Order detail (with items)")
    @GetMapping("/detail")
    public Result<?> detail(@RequestParam Long orderId) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return Result.fail("鏈櫥褰?");
        }
        var detail = orderBizService.detail(loginUser.getUserId(), orderId);
        return detail == null ? Result.fail("order not found") : Result.ok(detail);
    }
}
