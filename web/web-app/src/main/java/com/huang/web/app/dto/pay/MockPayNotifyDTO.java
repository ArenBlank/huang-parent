package com.huang.web.app.dto.pay;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Local mock payment notify request")
public class MockPayNotifyDTO {

    @NotBlank
    @Schema(description = "Existing pay number", example = "CPAY202604101122334455")
    private String payNo;

    @Schema(description = "Mock trade number, generated when omitted", example = "MOCK-TRADE-001")
    private String tradeNo;
}
