package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文章审核记录表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "文章审核记录表")
@TableName(value = "article_audit_log")
@Data
public class ArticleAuditLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文章ID")
    @TableField(value = "article_id")
    private Long articleId;

    @Schema(description = "操作类型: submit提交 approve通过 reject拒绝 offline下线")
    @TableField(value = "operation")
    private String operation;

    @Schema(description = "操作人ID")
    @TableField(value = "operator_id")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    @TableField(value = "operator_name")
    private String operatorName;

    @Schema(description = "操作人角色: coach教练 admin管理员")
    @TableField(value = "operator_role")
    private String operatorRole;

    @Schema(description = "操作前状态")
    @TableField(value = "before_status")
    private String beforeStatus;

    @Schema(description = "操作后状态")
    @TableField(value = "after_status")
    private String afterStatus;

    @Schema(description = "操作备注")
    @TableField(value = "remark")
    private String remark;
}