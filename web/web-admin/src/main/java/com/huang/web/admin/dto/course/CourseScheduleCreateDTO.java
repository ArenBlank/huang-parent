package com.huang.web.admin.dto.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseScheduleCreateDTO {

    @NotNull
    @Min(1)
    private Long courseId;

    @NotNull
    @Min(1)
    private Long coachId;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    @NotNull
    @Min(1)
    private Integer capacity;

    @NotNull
    private Integer status;
}
