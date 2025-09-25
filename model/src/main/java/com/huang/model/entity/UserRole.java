package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户角色关联表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "用户角色关联表")
@TableName(value = "user_role")
@Data
public class UserRole extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "角色ID")
    @TableField(value = "role_id")
    private Long roleId;
}
