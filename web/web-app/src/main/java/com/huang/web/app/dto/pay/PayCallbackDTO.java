package com.huang.web.app.dto.pay;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayCallbackDTO {

    @NotBlank
    private String payNo;

    @NotBlank
    private String tradeNo;

    @NotBlank
    private String status;

    private BigDecimal amount;

    @NotBlank
    private String sign;
}

