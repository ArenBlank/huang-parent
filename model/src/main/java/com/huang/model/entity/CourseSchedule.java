package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "课程排期")
@TableName(value = "course_schedule")
@Data
public class CourseSchedule extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "课程ID")
    @TableField(value = "course_id")
    private Long courseId;

    @Schema(description = "教练ID")
    @TableField(value = "coach_id")
    private Long coachId;

    @Schema(description = "开始时间")
    @TableField(value = "start_time")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @TableField(value = "end_time")
    private LocalDateTime endTime;

    @Schema(description = "容量")
    @TableField(value = "capacity")
    private Integer capacity;

    @Schema(description = "已报名人数")
    @TableField(value = "booked_count")
    private Integer bookedCount;

    @Schema(description = "状态:0关闭 1可报名")
    @TableField(value = "status")
    private Integer status;
}
