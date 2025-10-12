package com.huang.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * Admin端教练日程查询DTO
 * @author huang
 * @since 2025-01-24
 */
@Schema(description = "Admin端教练日程查询DTO")
@Data
public class AdminCoachScheduleQueryDTO {

    @Schema(description = "当前页码", example = "1")
    private Integer current = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer size = 10;

    @Schema(description = "教练ID")
    private Long coachId;

    @Schema(description = "教练姓名")
    private String coachName;

    @Schema(description = "状态: pending待审核 approved已批准 rejected已拒绝")
    private String status;

    @Schema(description = "变更类型: leave请假 overtime加班 adjust调整")
    private String changeType;

    @Schema(description = "申请开始日期")
    private LocalDate applyStartDate;

    @Schema(description = "申请结束日期")
    private LocalDate applyEndDate;

    @Schema(description = "原日期开始范围")
    private LocalDate originalStartDate;

    @Schema(description = "原日期结束范围")
    private LocalDate originalEndDate;
}