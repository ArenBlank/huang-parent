package com.huang.web.admin.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 账号注销申请审核DTO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "账号注销申请审核请求")
@Data
public class AccountCancelReviewDTO {

    @Schema(description = "审核结果: approved已批准 rejected已拒绝", requiredMode = Schema.RequiredMode.REQUIRED, example = "approved")
    @NotBlank(message = "审核结果不能为空")
    @Pattern(regexp = "^(approved|rejected)$", message = "审核结果只能为approved或rejected")
    private String reviewStatus;

    @Schema(description = "审核备注", example = "审核通过，同意注销申请")
    private String reviewRemark;
}