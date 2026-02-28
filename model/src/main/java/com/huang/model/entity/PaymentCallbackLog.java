package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "Payment callback audit log")
@TableName(value = "payment_callback_log")
@Data
public class PaymentCallbackLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField(value = "pay_no")
    private String payNo;

    @TableField(value = "order_id")
    private Long orderId;

    @TableField(value = "channel_trade_no")
    private String channelTradeNo;

    @TableField(value = "callback_status")
    private String callbackStatus;

    @TableField(value = "sign_valid")
    private Integer signValid;

    @TableField(value = "callback_payload")
    private String callbackPayload;

    @TableField(value = "process_result")
    private String processResult;

    @TableField(value = "error_message")
    private String errorMessage;

    @TableField(value = "notified_at")
    private LocalDateTime notifiedAt;
}

