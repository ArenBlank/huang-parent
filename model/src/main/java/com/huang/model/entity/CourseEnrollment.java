package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "课程报名")
@TableName(value = "course_enrollment")
@Data
public class CourseEnrollment extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "课程ID")
    @TableField(value = "course_id")
    private Long courseId;

    @Schema(description = "排期ID")
    @TableField(value = "schedule_id")
    private Long scheduleId;

    @Schema(description = "订单ID")
    @TableField(value = "order_id")
    private Long orderId;

    @Schema(description = "状态:0取消 1已报名")
    @TableField(value = "status")
    private Integer status;

    @Schema(description = "报名时间")
    @TableField(value = "enroll_time")
    private LocalDateTime enrollTime;
}
