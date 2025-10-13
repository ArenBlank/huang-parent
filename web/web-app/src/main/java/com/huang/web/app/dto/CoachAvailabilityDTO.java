package com.huang.web.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalTime;

/**
 * 教练可用性设置DTO
 * @author huang
 * @since 2025-01-24
 */
@Schema(description = "教练可用性设置DTO")
@Data
public class CoachAvailabilityDTO {

    @Schema(description = "星期几(1-7)", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "星期几不能为空")
    @Min(value = 1, message = "星期几必须是1-7")
    @Max(value = 7, message = "星期几必须是1-7")
    private Integer dayOfWeek;

    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "09:00:00")
    @NotNull(message = "开始时间不能为空")
    private LocalTime startTime;

    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "18:00:00")
    @NotNull(message = "结束时间不能为空")
    private LocalTime endTime;

    @Schema(description = "是否可用: 1可用 0不可用", example = "1")
    private Integer isAvailable = 1;
}