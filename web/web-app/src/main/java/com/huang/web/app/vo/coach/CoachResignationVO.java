package com.huang.web.app.vo.coach;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * App端教练离职申请VO
 * @author system
 * @since 2025-01-25
 */
@Schema(description = "教练离职申请VO")
@Data
public class CoachResignationVO {

    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "教练ID")
    private Long coachId;

    @Schema(description = "教练姓名")
    private String coachName;

    @Schema(description = "预计离职日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate resignationDate;

    @Schema(description = "离职原因")
    private String reason;

    @Schema(description = "交接计划")
    private String handoverPlan;

    @Schema(description = "申请状态: pending待审核 approved已批准 rejected已拒绝 cancelled已取消")
    private String status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "申请时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;

    @Schema(description = "审核时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;

    @Schema(description = "审核备注")
    private String reviewRemark;

    @Schema(description = "实际离职日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualLeaveDate;

    @Schema(description = "是否可撤销: true是 false否")
    private Boolean canCancel;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
