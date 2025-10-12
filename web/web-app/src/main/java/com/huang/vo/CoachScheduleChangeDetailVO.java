package com.huang.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 教练日程变更申请详情VO
 * @author huang
 * @since 2025-01-24
 */
@Schema(description = "教练日程变更申请详情VO")
@Data
public class CoachScheduleChangeDetailVO {

    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "教练ID")
    private Long coachId;

    @Schema(description = "变更类型: leave请假 overtime加班 adjust调整")
    private String changeType;

    @Schema(description = "变更类型名称")
    private String changeTypeName;

    @Schema(description = "原日期")
    private LocalDate originalDate;

    @Schema(description = "原开始时间")
    private LocalTime originalStartTime;

    @Schema(description = "原结束时间")
    private LocalTime originalEndTime;

    @Schema(description = "新日期")
    private LocalDate newDate;

    @Schema(description = "新开始时间")
    private LocalTime newStartTime;

    @Schema(description = "新结束时间")
    private LocalTime newEndTime;

    @Schema(description = "变更原因")
    private String reason;

    @Schema(description = "状态: pending待审核 approved已批准 rejected已拒绝")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "申请时间")
    private LocalDateTime applyTime;

    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;

    @Schema(description = "审核人ID")
    private Long reviewerId;

    @Schema(description = "审核备注")
    private String reviewRemark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}