package com.huang.web.admin.dto.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * 教练离职申请审核DTO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "教练离职申请审核DTO")
@Data
public class CoachResignationReviewDTO {

    @Schema(description = "审核备注", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "审核备注不能为空")
    private String reviewRemark;

    @Schema(description = "实际离职日期（批准时需要）")
    private LocalDate actualLeaveDate;
}
