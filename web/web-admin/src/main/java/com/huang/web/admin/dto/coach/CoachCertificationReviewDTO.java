package com.huang.web.admin.dto.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 教练认证申请审核DTO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "教练认证申请审核DTO")
@Data
public class CoachCertificationReviewDTO {

    @Schema(description = "申请ID", required = true)
    @NotNull(message = "申请ID不能为空")
    private Long id;

    @Schema(description = "审核结果: approved已通过 rejected已拒绝", required = true)
    @NotNull(message = "审核结果不能为空")
    private String status;

    @Schema(description = "审核备注", example = "申请材料齐全，符合教练认证要求")
    @Size(max = 500, message = "审核备注不能超过500字")
    private String reviewRemark;

    @Schema(description = "认证编号（通过时自动生成，可自定义）")
    @Size(max = 50, message = "认证编号不能超过50字符")
    private String certificationNo;
}