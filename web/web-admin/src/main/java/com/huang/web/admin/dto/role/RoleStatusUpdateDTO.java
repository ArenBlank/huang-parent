package com.huang.web.admin.dto.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

/**
 * 角色状态更新DTO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "角色状态更新请求")
@Data
public class RoleStatusUpdateDTO {

    @Schema(description = "角色ID", required = true)
    @NotNull(message = "角色ID不能为空")
    @Min(value = 1, message = "角色ID必须大于0")
    private Long roleId;

    @Schema(description = "状态：0-禁用，1-正常", required = true)
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}