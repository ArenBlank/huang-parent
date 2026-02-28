package com.huang.web.app.dto.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseRefundDTO {

    @NotNull
    @Min(1)
    private Long enrollmentId;

    @NotBlank
    private String reason;
}
