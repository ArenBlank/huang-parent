package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 教练档期
 * @author system
 * @since 2026-02-25
 */
@Schema(description = "教练档期")
@TableName(value = "coach_schedule")
@Data
public class CoachSchedule extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "教练ID")
    @TableField(value = "coach_id")
    private Long coachId;

    @Schema(description = "日期")
    @TableField(value = "schedule_date")
    private LocalDate scheduleDate;

    @Schema(description = "开始时间")
    @TableField(value = "start_time")
    private LocalTime startTime;

    @Schema(description = "结束时间")
    @TableField(value = "end_time")
    private LocalTime endTime;

    @Schema(description = "价格")
    @TableField(value = "price")
    private BigDecimal price;

    @Schema(description = "容量")
    @TableField(value = "capacity")
    private Integer capacity;

    @Schema(description = "已预约人数")
    @TableField(value = "booked_count")
    private Integer bookedCount;

    @Schema(description = "状态")
    @TableField(value = "status")
    private Integer status;
}
