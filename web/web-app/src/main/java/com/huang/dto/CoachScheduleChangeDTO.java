package com.huang.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 教练日程变更申请DTO
 * @author huang
 * @since 2025-01-24
 */
@Schema(description = "教练日程变更申请DTO")
@Data
public class CoachScheduleChangeDTO {

    @Schema(description = "变更类型: leave请假 overtime加班 adjust调整", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "变更类型不能为空")
    private String changeType;

    @Schema(description = "原日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "原日期不能为空")
    private LocalDate originalDate;

    @Schema(description = "原开始时间")
    private LocalTime originalStartTime;

    @Schema(description = "原结束时间")
    private LocalTime originalEndTime;

    @Schema(description = "新日期")
    private LocalDate newDate;

    @Schema(description = "新开始时间")
    private LocalTime newStartTime;

    @Schema(description = "新结束时间")
    private LocalTime newEndTime;

    @Schema(description = "变更原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "变更原因不能为空")
    private String reason;
}