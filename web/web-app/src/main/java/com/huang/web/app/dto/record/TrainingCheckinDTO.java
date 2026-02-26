package com.huang.web.app.dto.record;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TrainingCheckinDTO {

    @NotNull
    @Min(1)
    private Long planId;

    @NotNull
    @Min(1)
    private Long planItemId;

    @NotNull
    private LocalDate recordDate;

    private Integer durationMin;

    private Integer calories;

    private String feeling;

    private String recordImages;
}
