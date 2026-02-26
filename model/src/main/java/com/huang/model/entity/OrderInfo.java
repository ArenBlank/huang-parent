package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单
 * @author system
 * @since 2026-02-25
 */
@Schema(description = "订单")
@TableName(value = "order_info")
@Data
public class OrderInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单号")
    @TableField(value = "order_no")
    private String orderNo;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "总金额")
    @TableField(value = "total_amount")
    private BigDecimal totalAmount;

    @Schema(description = "订单状态")
    @TableField(value = "order_status")
    private String orderStatus;

    @Schema(description = "支付状态")
    @TableField(value = "pay_status")
    private String payStatus;

    @Schema(description = "业务类型")
    @TableField(value = "biz_type")
    private String bizType;

    @Schema(description = "业务ID")
    @TableField(value = "biz_id")
    private Long bizId;
}
