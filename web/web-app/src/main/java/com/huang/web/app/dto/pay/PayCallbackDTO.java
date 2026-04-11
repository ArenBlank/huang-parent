package com.huang.web.app.dto.pay;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Third-party payment callback payload")
public class PayCallbackDTO {

    @NotBlank
    @Schema(description = "Platform pay number", example = "CPAY202604101122334455")
    private String payNo;

    @NotBlank
    @Schema(description = "Channel trade number", example = "WX-TRADE-20260410-001")
    private String tradeNo;

    @NotBlank
    @Schema(description = "Payment status", example = "SUCCESS")
    private String status;

    @Schema(description = "Paid amount", example = "59.90")
    private BigDecimal amount;

    @NotBlank
    @Schema(description = "Callback signature", example = "mock-sign")
    private String sign;
}
