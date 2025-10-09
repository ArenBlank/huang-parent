package com.huang.web.admin.dto.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 教练离职申请查询DTO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "教练离职申请查询DTO")
@Data
public class CoachResignationQueryDTO {

    @Schema(description = "当前页码")
    private Long current = 1L;

    @Schema(description = "每页大小")
    private Long size = 10L;

    @Schema(description = "教练ID")
    private Long coachId;

    @Schema(description = "教练姓名")
    private String coachName;

    @Schema(description = "申请状态: pending待审核 approved已批准 rejected已拒绝 cancelled已取消")
    private String status;

    @Schema(description = "申请开始时间")
    private LocalDate applyStartDate;

    @Schema(description = "申请结束时间")
    private LocalDate applyEndDate;

    @Schema(description = "预计离职开始日期")
    private LocalDate resignationStartDate;

    @Schema(description = "预计离职结束日期")
    private LocalDate resignationEndDate;
}
