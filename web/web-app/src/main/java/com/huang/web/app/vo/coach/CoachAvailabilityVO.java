package com.huang.web.app.vo.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 教练可用性VO
 * @author huang
 * @since 2025-01-24
 */
@Schema(description = "教练可用性VO")
@Data
public class CoachAvailabilityVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "教练ID")
    private Long coachId;

    @Schema(description = "星期几(1-7)")
    private Integer dayOfWeek;

    @Schema(description = "星期名称")
    private String dayOfWeekName;

    @Schema(description = "开始时间")
    private LocalTime startTime;

    @Schema(description = "结束时间")
    private LocalTime endTime;

    @Schema(description = "是否可用: 1可用 0不可用")
    private Integer isAvailable;

    @Schema(description = "可用状态名称")
    private String availableStatusName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}