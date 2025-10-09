package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 教练预约时段表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "教练预约时段表")
@TableName(value = "coach_booking_slot")
@Data
public class CoachBookingSlot extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "教练ID")
    @TableField(value = "coach_id")
    private Long coachId;

    @Schema(description = "服务项目ID")
    @TableField(value = "service_id")
    private Long serviceId;

    @Schema(description = "预约日期")
    @TableField(value = "booking_date")
    private LocalDate bookingDate;

    @Schema(description = "开始时间")
    @TableField(value = "start_time")
    private LocalTime startTime;

    @Schema(description = "结束时间")
    @TableField(value = "end_time")
    private LocalTime endTime;

    @Schema(description = "状态: available可预约 booked已预约 blocked已屏蔽 completed已完成")
    @TableField(value = "status")
    private String status;

    @Schema(description = "预约用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "关联订单ID")
    @TableField(value = "order_id")
    private Long orderId;

    @Schema(description = "预约价格")
    @TableField(value = "booking_price")
    private BigDecimal bookingPrice;

    @Schema(description = "预约时间")
    @TableField(value = "booking_time")
    private LocalDateTime bookingTime;

    @Schema(description = "完成时间")
    @TableField(value = "completion_time")
    private LocalDateTime completionTime;

    @Schema(description = "取消时间")
    @TableField(value = "cancellation_time")
    private LocalDateTime cancellationTime;

    @Schema(description = "取消原因")
    @TableField(value = "cancellation_reason")
    private String cancellationReason;

    @Schema(description = "备注")
    @TableField(value = "remark")
    private String remark;
}