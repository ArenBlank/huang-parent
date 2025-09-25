package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "角色表")
@TableName(value = "role")
@Data
public class Role extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色名称")
    @TableField(value = "role_name")
    private String roleName;

    @Schema(description = "角色编码")
    @TableField(value = "role_code")
    private String roleCode;

    @Schema(description = "描述")
    @TableField(value = "description")
    private String description;

    @Schema(description = "状态：0-禁用，1-正常")
    @TableField(value = "status")
    private Integer status;
}