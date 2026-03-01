package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Operation log.
 */
@Schema(description = "操作日志")
@TableName(value = "operation_log")
@Data
public class OperationLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "操作人ID")
    @TableField(value = "operator_id")
    private Long operatorId;

    @Schema(description = "模块")
    @TableField(value = "module")
    private String module;

    @Schema(description = "动作")
    @TableField(value = "action")
    private String action;

    @Schema(description = "详情")
    @TableField(value = "detail")
    private String detail;

    @Schema(description = "IP")
    @TableField(value = "ip")
    private String ip;

    @Schema(description = "是否成功")
    @TableField(value = "success")
    private Integer success;
}
