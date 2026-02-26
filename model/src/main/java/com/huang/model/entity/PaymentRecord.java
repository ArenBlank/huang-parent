package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录表
 * @author system
 * @since 2026-02-25
 */
@Schema(description = "支付记录表")
@TableName(value = "payment_record")
@Data
public class PaymentRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    @TableField(value = "order_id")
    private Long orderId;

    @Schema(description = "支付单号")
    @TableField(value = "pay_no")
    private String payNo;

    @Schema(description = "回调幂等键")
    @TableField(value = "callback_idempotency_key")
    private String callbackIdempotencyKey;

    @Schema(description = "支付渠道")
    @TableField(value = "pay_channel")
    private String payChannel;

    @Schema(description = "支付金额")
    @TableField(value = "pay_amount")
    private BigDecimal payAmount;

    @Schema(description = "支付状态")
    @TableField(value = "pay_status")
    private String payStatus;

    @Schema(description = "支付完成时间")
    @TableField(value = "pay_time")
    private LocalDateTime payTime;

}
