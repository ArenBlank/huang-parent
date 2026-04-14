package com.huang.web.admin.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseCheckInDTO {

    @NotBlank
    @Size(min = 6, max = 6)
    private String checkInCode;
}
