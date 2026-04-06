package com.huang.web.admin.dto.plan;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TrainingPlanUpsertDTO {

    @NotBlank
    private String title;

    private String goal;

    private String level;

    @NotNull
    @Min(1)
    private Integer durationWeeks;

    private String coverUrl;

    @NotNull
    private Integer status;
}
