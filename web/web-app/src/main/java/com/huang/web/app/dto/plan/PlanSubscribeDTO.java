package com.huang.web.app.dto.plan;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PlanSubscribeDTO {

    @NotNull
    @Min(1)
    private Long planId;

    @NotNull
    private LocalDate startDate;
}
