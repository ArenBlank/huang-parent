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

    @Schema(description = "交易状态 0取消 1未支付 2已支付 3已退款")
    @TableField(value = "status")
    private Integer status;

    @Schema(description = "履约状态 0待上课 1已签到/已完成 2已缺席 3已失效/已取消")
    @TableField(value = "attend_status")
    private Integer attendStatus;

    @Schema(description = "6位核销码")
    @TableField(value = "check_in_code")
    private String checkInCode;

    @Schema(description = "报名时间")
    @TableField(value = "enroll_time")
    private LocalDateTime enrollTime;
}
