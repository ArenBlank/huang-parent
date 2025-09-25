package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程报名表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "课程报名表")
@TableName(value = "course_enrollment")
@Data
public class CourseEnrollment extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "排期ID")
    @TableField(value = "schedule_id")
    private Long scheduleId;

    @Schema(description = "报名时间")
    @TableField(value = "enrollment_time")
    private LocalDateTime enrollmentTime;

    @Schema(description = "状态：0-已取消，1-已报名，2-已签到")
    @TableField(value = "status")
    private Integer status;

    @Schema(description = "签到时间")
    @TableField(value = "check_in_time")
    private LocalDateTime checkInTime;
}