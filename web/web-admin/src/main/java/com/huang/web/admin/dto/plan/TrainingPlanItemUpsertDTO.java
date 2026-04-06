package com.huang.web.admin.dto.plan;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TrainingPlanItemUpsertDTO {

    @NotNull
    @Min(1)
    private Integer dayIndex;

    @NotBlank
    private String actionName;

    private Integer sets;

    private Integer reps;

    private Integer durationMin;

    private Integer restSec;

    private Long videoId;

    private Integer sort;
}
