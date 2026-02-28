package com.huang.web.app.controller;

import com.huang.common.result.Result;
import com.huang.web.app.dto.pay.MockPayNotifyDTO;
import com.huang.web.app.dto.pay.PayCallbackDTO;
import com.huang.web.app.service.biz.PaymentCallbackBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "App Payment Callback", description = "Async callback and local mock callback gateway")
@RestController
@RequestMapping("/app/pay")
public class PaymentCallbackController {

    private final PaymentCallbackBizService paymentCallbackBizService;

    public PaymentCallbackController(PaymentCallbackBizService paymentCallbackBizService) {
        this.paymentCallbackBizService = paymentCallbackBizService;
    }

    @Operation(summary = "Async payment callback (third-party notifies this endpoint)")
    @PostMapping("/callback")
    public String callback(@Valid @RequestBody PayCallbackDTO dto) {
        return paymentCallbackBizService.handleCallback(dto, dto.toString());
    }

    @Operation(summary = "Mock third-party notify (for local demo)")
    @PostMapping("/mock-notify")
    public Result<?> mockNotify(@Valid @RequestBody MockPayNotifyDTO dto) {
        PayCallbackDTO callbackDTO = paymentCallbackBizService.buildMockCallback(dto.getPayNo(), dto.getTradeNo());
        if (callbackDTO == null) {
            return Result.fail("payNo not found");
        }
        String result = paymentCallbackBizService.handleCallback(callbackDTO, callbackDTO.toString());
        return Result.ok(result);
    }
}

