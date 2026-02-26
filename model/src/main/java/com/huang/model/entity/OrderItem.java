package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单项
 * @author system
 * @since 2026-02-25
 */
@Schema(description = "订单项")
@TableName(value = "order_item")
@Data
public class OrderItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    @TableField(value = "order_id")
    private Long orderId;

    @Schema(description = "项目类型")
    @TableField(value = "item_type")
    private String itemType;

    @Schema(description = "项目ID")
    @TableField(value = "item_id")
    private Long itemId;

    @Schema(description = "项目名称")
    @TableField(value = "item_name")
    private String itemName;

    @Schema(description = "价格")
    @TableField(value = "price")
    private BigDecimal price;

    @Schema(description = "数量")
    @TableField(value = "quantity")
    private Integer quantity;

    @Schema(description = "金额")
    @TableField(value = "amount")
    private BigDecimal amount;
}
