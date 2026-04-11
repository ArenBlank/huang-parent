package com.huang.web.app.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Course enrollment request")
public class CourseEnrollDTO {

    @NotNull
    @Min(1)
    @Schema(description = "Course schedule ID", example = "2")
    private Long scheduleId;
}
