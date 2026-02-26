package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 教练档案
 * @author system
 * @since 2026-02-25
 */
@Schema(description = "教练档案")
@TableName(value = "coach_profile")
@Data
public class CoachProfile extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "简介")
    @TableField(value = "bio")
    private String bio;

    @Schema(description = "擅长领域")
    @TableField(value = "expertise")
    private String expertise;

    @Schema(description = "教龄")
    @TableField(value = "years")
    private Integer years;

    @Schema(description = "基础价格")
    @TableField(value = "price")
    private BigDecimal price;

    @Schema(description = "评分")
    @TableField(value = "rating")
    private BigDecimal rating;

    @Schema(description = "认证状态")
    @TableField(value = "cert_status")
    private Integer certStatus;

    @Schema(description = "状态")
    @TableField(value = "status")
    private Integer status;
}
