package com.huang.web.app.dto.plan;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlanUnsubscribeDTO {

    @NotNull
    @Min(1)
    private Long planId;
}
