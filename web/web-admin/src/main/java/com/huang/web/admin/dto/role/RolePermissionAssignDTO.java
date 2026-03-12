package com.huang.web.admin.dto.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "Role permission assignment request")
@Data
public class RolePermissionAssignDTO {

    @Schema(description = "Role ID", required = true)
    @NotNull(message = "roleId is required")
    private Long roleId;

    @Schema(description = "Permission codes")
    private List<String> permCodes;

    @Schema(description = "Operation: replace/add/remove")
    private String operation = "replace";
}
