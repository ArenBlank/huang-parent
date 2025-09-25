package com.huang.web.admin.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

/**
 * 用户状态更新DTO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "用户状态更新请求")
@Data
public class UserStatusUpdateDTO {

    @Schema(description = "用户ID", required = true)
    @NotNull(message = "用户ID不能为空")
    @Min(value = 1, message = "用户ID必须大于0")
    private Long userId;

    @Schema(description = "状态：0-禁用，1-正常", required = true)
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}