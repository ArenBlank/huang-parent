package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalTime;

/**
 * 教练可用时间表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "教练可用时间表")
@TableName(value = "coach_availability")
@Data
public class CoachAvailability extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "教练ID")
    @TableField(value = "coach_id")
    private Long coachId;

    @Schema(description = "星期几(1-7)")
    @TableField(value = "day_of_week")
    private Integer dayOfWeek;

    @Schema(description = "开始时间")
    @TableField(value = "start_time")
    private LocalTime startTime;

    @Schema(description = "结束时间")
    @TableField(value = "end_time")
    private LocalTime endTime;

    @Schema(description = "是否可用")
    @TableField(value = "is_available")
    private Integer isAvailable;
}