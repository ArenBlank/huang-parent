package com.huang.web.admin.vo.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 教练离职申请详情VO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "教练离职申请详情VO")
@Data
public class CoachResignationDetailVO {

    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "教练ID")
    private Long coachId;

    @Schema(description = "教练姓名")
    private String coachName;

    @Schema(description = "教练手机号")
    private String coachPhone;

    @Schema(description = "教练邮箱")
    private String coachEmail;

    @Schema(description = "预计离职日期")
    private LocalDate resignationDate;

    @Schema(description = "离职原因")
    private String reason;

    @Schema(description = "工作交接计划")
    private String handoverPlan;

    @Schema(description = "申请状态: pending待审核 approved已批准 rejected已拒绝 cancelled已取消")
    private String status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "申请时间")
    private LocalDateTime applyTime;

    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;

    @Schema(description = "审核人ID")
    private Long reviewerId;

    @Schema(description = "审核人姓名")
    private String reviewerName;

    @Schema(description = "审核备注")
    private String reviewRemark;

    @Schema(description = "实际离职日期")
    private LocalDate actualLeaveDate;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "是否可以审核")
    private Boolean canReview;

    @Schema(description = "是否可以取消")
    private Boolean canCancel;
}