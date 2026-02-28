package com.huang.web.app.dto.pay;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MockPayNotifyDTO {

    @NotBlank
    private String payNo;

    private String tradeNo;
}

