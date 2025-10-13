package com.huang.web.admin.dto.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 教练日程变更审核DTO
 * @author huang
 * @since 2025-01-24
 */
@Schema(description = "教练日程变更审核DTO")
@Data
public class CoachScheduleReviewDTO {

    @Schema(description = "申请ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "申请ID不能为空")
    private Long id;

    @Schema(description = "审核结果: approved已批准 rejected已拒绝", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "审核结果不能为空")
    private String status;

    @Schema(description = "审核备注", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "审核备注不能为空")
    private String reviewRemark;
}