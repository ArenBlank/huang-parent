package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Role-permission mapping entity.
 */
@Schema(description = "Role permission mapping")
@TableName(value = "role_permission")
@Data
public class RolePermission extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Role ID")
    @TableField(value = "role_id")
    private Long roleId;

    @Schema(description = "Permission ID")
    @TableField(value = "permission_id")
    private Long permId;
}
