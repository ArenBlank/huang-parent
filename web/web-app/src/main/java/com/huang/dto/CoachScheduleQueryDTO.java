package com.huang.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 教练日程查询DTO
 * @author huang
 * @since 2025-01-24
 */
@Schema(description = "教练日程查询DTO")
@Data
public class CoachScheduleQueryDTO {

    @Schema(description = "当前页码", example = "1")
    private Integer current = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer size = 10;

    @Schema(description = "状态: pending待审核 approved已批准 rejected已拒绝")
    private String status;

    @Schema(description = "变更类型: leave请假 overtime加班 adjust调整")
    private String changeType;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;
}