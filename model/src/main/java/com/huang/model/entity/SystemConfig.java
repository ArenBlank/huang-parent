package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 系统配置表
 * @author system
 * @since 2026-02-25
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

    @Schema(description = "备注")
    @TableField(value = "remark")
    private String remark;

}
