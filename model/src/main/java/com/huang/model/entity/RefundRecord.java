package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录
 * @author system
 * @since 2026-02-25
 */
@Schema(description = "退款记录")
@TableName(value = "refund_record")
@Data
public class RefundRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    @TableField(value = "order_id")
    private Long orderId;

    @Schema(description = "退款单号")
    @TableField(value = "refund_no")
    private String refundNo;

    @Schema(description = "退款金额")
    @TableField(value = "refund_amount")
    private BigDecimal refundAmount;

    @Schema(description = "退款状态")
    @TableField(value = "refund_status")
    private String refundStatus;

    @Schema(description = "退款时间")
    @TableField(value = "refund_time")
    private LocalDateTime refundTime;

    @Schema(description = "原因")
    @TableField(value = "reason")
    private String reason;
}
