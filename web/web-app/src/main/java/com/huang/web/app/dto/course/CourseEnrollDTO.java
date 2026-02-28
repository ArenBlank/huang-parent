package com.huang.web.app.dto.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseEnrollDTO {

    @NotNull
    @Min(1)
    private Long scheduleId;
}
