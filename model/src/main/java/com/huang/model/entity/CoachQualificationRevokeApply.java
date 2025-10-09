package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 教练资格撤销申请表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "教练资格撤销申请表")
@TableName(value = "coach_qualification_revoke_apply")
@Data
public class CoachQualificationRevokeApply extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "教练ID")
    @TableField(value = "coach_id")
    private Long coachId;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "撤销原因")
    @TableField(value = "revoke_reason")
    private String revokeReason;

    @Schema(description = "期望生效日期")
    @TableField(value = "effective_date")
    private LocalDate effectiveDate;

    @Schema(description = "状态: pending待审核 approved已批准 rejected已拒绝 cancelled已取消")
    @TableField(value = "status")
    private String status;

    @Schema(description = "申请时间")
    @TableField(value = "apply_time")
    private LocalDateTime applyTime;

    @Schema(description = "审核时间")
    @TableField(value = "review_time")
    private LocalDateTime reviewTime;

    @Schema(description = "审核人ID")
    @TableField(value = "reviewer_id")
    private Long reviewerId;

    @Schema(description = "审核备注")
    @TableField(value = "review_remark")
    private String reviewRemark;

    @Schema(description = "实际撤销日期")
    @TableField(value = "actual_revoke_date")
    private LocalDate actualRevokeDate;
}