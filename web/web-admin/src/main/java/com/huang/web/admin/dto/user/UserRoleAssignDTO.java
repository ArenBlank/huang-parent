package com.huang.web.admin.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * 用户角色分配DTO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "用户角色分配请求")
@Data
public class UserRoleAssignDTO {

    @Schema(description = "用户ID", required = true)
    @NotNull(message = "用户ID不能为空")
    @Min(value = 1, message = "用户ID必须大于0")
    private Long userId;

    @Schema(description = "角色ID列表", required = true)
    @NotEmpty(message = "角色ID列表不能为空")
    private List<Long> roleIds;

    @Schema(description = "备注")
    private String remark;
}