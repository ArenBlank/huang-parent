package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程排期表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "课程排期表")
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

    @Schema(description = "上课地点")
    @TableField(value = "location")
    private String location;

    @Schema(description = "当前报名人数")
    @TableField(value = "current_participants")
    private Integer currentParticipants;

    @Schema(description = "状态：0-已取消，1-正常，2-已结束")
    @TableField(value = "status")
    private Integer status;
}