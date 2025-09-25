package com.huang.web.admin.dto.userrole;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量角色分配DTO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "批量角色分配请求")
@Data
public class BatchRoleAssignDTO {

    @Schema(description = "用户ID列表", required = true)
    @NotEmpty(message = "用户ID列表不能为空")
    private List<Long> userIds;

    @Schema(description = "角色ID列表", required = true)
    @NotEmpty(message = "角色ID列表不能为空")
    private List<Long> roleIds;

    @Schema(description = "操作类型：add-新增角色，replace-替换角色，remove-移除角色", required = true)
    private String operation = "replace";

    @Schema(description = "备注")
    private String remark;
}