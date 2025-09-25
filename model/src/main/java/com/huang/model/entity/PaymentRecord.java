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
 * @since 2025-09-23
 */
@Schema(description = "支付记录表")
@TableName(value = "payment_record")
@Data
public class PaymentRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "商户ID(教练/平台)")
    @TableField(value = "merchant_id")
    private Long merchantId;

    @Schema(description = "订单号")
    @TableField(value = "order_no")
    private String orderNo;

    @Schema(description = "业务订单ID")
    @TableField(value = "order_id")
    private String orderId;

    @Schema(description = "订单类型(1咨询 2课程 3会员 4其他)")
    @TableField(value = "order_type")
    private Byte orderType;

    @Schema(description = "支付方式(1微信 2支付宝 3银行卡 4余额)")
    @TableField(value = "payment_method")
    private Byte paymentMethod;

    @Schema(description = "第三方交易号")
    @TableField(value = "third_party_trade_no")
    private String thirdPartyTradeNo;

    @Schema(description = "支付金额")
    @TableField(value = "amount")
    private BigDecimal amount;

    @Schema(description = "实际支付金额")
    @TableField(value = "actual_amount")
    private BigDecimal actualAmount;

    @Schema(description = "优惠金额")
    @TableField(value = "discount_amount")
    private BigDecimal discountAmount;

    @Schema(description = "优惠券抵扣")
    @TableField(value = "coupon_discount")
    private BigDecimal couponDiscount;

    @Schema(description = "积分抵扣")
    @TableField(value = "points_discount")
    private BigDecimal pointsDiscount;

    @Schema(description = "货币类型")
    @TableField(value = "currency")
    private String currency;

    @Schema(description = "汇率")
    @TableField(value = "exchange_rate")
    private BigDecimal exchangeRate;

    @Schema(description = "支付状态(1待支付 2支付中 3支付成功 4支付失败 5已退款)")
    @TableField(value = "payment_status")
    private Byte paymentStatus;

    @Schema(description = "支付发起时间")
    @TableField(value = "pay_start_time")
    private LocalDateTime payStartTime;

    @Schema(description = "支付完成时间")
    @TableField(value = "pay_time")
    private LocalDateTime payTime;

    @Schema(description = "退款金额")
    @TableField(value = "refund_amount")
    private BigDecimal refundAmount;

    @Schema(description = "退款时间")
    @TableField(value = "refund_time")
    private LocalDateTime refundTime;

    @Schema(description = "退款原因")
    @TableField(value = "refund_reason")
    private String refundReason;

    @Schema(description = "失败原因")
    @TableField(value = "failure_reason")
    private String failureReason;

    @Schema(description = "通知状态(0未通知 1已通知)")
    @TableField(value = "notify_status")
    private Byte notifyStatus;

    @Schema(description = "备注")
    @TableField(value = "remarks")
    private String remarks;

}