package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 系统配置表
 * @author system
 * @since 2025-09-23
 */
@Schema(description = "系统配置表")
@TableName(value = "system_config")
@Data
public class SystemConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "配置键")
    @TableField(value = "config_key")
    private String configKey;

    @Schema(description = "配置值")
    @TableField(value = "config_value")
    private String configValue;

    @Schema(description = "配置类型(string/int/boolean/json)")
    @TableField(value = "config_type")
    private String configType;

    @Schema(description = "配置分组")
    @TableField(value = "config_group")
    private String configGroup;

    @Schema(description = "配置描述")
    @TableField(value = "config_desc")
    private String configDesc;

    @Schema(description = "是否加密")
    @TableField(value = "is_encrypted")
    private Byte isEncrypted;

    @Schema(description = "是否公开")
    @TableField(value = "is_public")
    private Byte isPublic;

    @Schema(description = "排序")
    @TableField(value = "sort_order")
    private Integer sortOrder;

    @Schema(description = "状态(0禁用 1启用)")
    @TableField(value = "status")
    private Byte status;

}