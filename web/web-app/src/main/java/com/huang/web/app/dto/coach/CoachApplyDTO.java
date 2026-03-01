package com.huang.web.app.dto.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "申请教练资料")
public class CoachApplyDTO {

    @NotBlank
    @Size(max = 500)
    @Schema(description = "个人简介")
    private String bio;

    @NotBlank
    @Size(max = 200)
    @Schema(description = "擅长领域，逗号分隔")
    private String expertise;

    @NotNull
    @Max(60)
    @Schema(description = "教学年限")
    private Integer years;

    @NotNull
    @DecimalMin("0.00")
    @Schema(description = "课时价格")
    private BigDecimal price;
}

