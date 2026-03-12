package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Permission entity (operation-level permission code).
 */
@Schema(description = "Permission")
@TableName(value = "permission")
@Data
public class Permission extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Permission name")
    @TableField(value = "perm_name")
    private String permName;

    @Schema(description = "Permission code")
    @TableField(value = "perm_code")
    private String permCode;

    @Schema(description = "Permission module")
    @TableField(value = "module")
    private String module;

    @Schema(description = "Status (0-disabled, 1-enabled)")
    @TableField(value = "status")
    private Integer status;
}
