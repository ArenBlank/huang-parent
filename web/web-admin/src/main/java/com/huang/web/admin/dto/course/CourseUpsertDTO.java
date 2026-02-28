package com.huang.web.admin.dto.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseUpsertDTO {

    @NotNull
    @Min(1)
    private Long categoryId;

    @NotBlank
    private String title;

    private String summary;

    private String coverUrl;

    private String level;

    @NotNull
    @Min(1)
    private Integer durationMin;

    @NotNull
    private BigDecimal price;

    @NotNull
    private Integer status;
}
