package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 教练日程变更申请表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "教练日程变更申请表")
@TableName(value = "coach_schedule_change")
@Data
public class CoachScheduleChange extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "教练ID")
    @TableField(value = "coach_id")
    private Long coachId;

    @Schema(description = "变更类型: leave请假 overtime加班 adjust调整")
    @TableField(value = "change_type")
    private String changeType;

    @Schema(description = "原日期")
    @TableField(value = "original_date")
    private LocalDate originalDate;

    @Schema(description = "原开始时间")
    @TableField(value = "original_start_time")
    private LocalTime originalStartTime;

    @Schema(description = "原结束时间")
    @TableField(value = "original_end_time")
    private LocalTime originalEndTime;

    @Schema(description = "新日期")
    @TableField(value = "new_date")
    private LocalDate newDate;

    @Schema(description = "新开始时间")
    @TableField(value = "new_start_time")
    private LocalTime newStartTime;

    @Schema(description = "新结束时间")
    @TableField(value = "new_end_time")
    private LocalTime newEndTime;

    @Schema(description = "变更原因")
    @TableField(value = "reason")
    private String reason;

    @Schema(description = "状态: pending待审核 approved已批准 rejected已拒绝")
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
}