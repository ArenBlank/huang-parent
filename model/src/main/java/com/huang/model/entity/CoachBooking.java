package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教练预约
 * @author system
 * @since 2026-02-25
 */
@Schema(description = "教练预约")
@TableName(value = "coach_booking")
@Data
public class CoachBooking extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "教练ID")
    @TableField(value = "coach_id")
    private Long coachId;

    @Schema(description = "档期ID")
    @TableField(value = "schedule_id")
    private Long scheduleId;

    @Schema(description = "订单ID")
    @TableField(value = "order_id")
    private Long orderId;

    @Schema(description = "预约状态")
    @TableField(value = "booking_status")
    private String bookingStatus;

    @Schema(description = "支付状态")
    @TableField(value = "pay_status")
    private String payStatus;

    @Schema(description = "到场时间")
    @TableField(value = "checkin_time")
    private LocalDateTime checkinTime;

    @Schema(description = "完成时间")
    @TableField(value = "finish_time")
    private LocalDateTime finishTime;
}
