package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 教练评价表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "教练评价表")
@TableName(value = "coach_evaluation")
@Data
public class CoachEvaluation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "教练ID")
    @TableField(value = "coach_id")
    private Long coachId;

    @Schema(description = "评价用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "服务ID")
    @TableField(value = "service_id")
    private Long serviceId;

    @Schema(description = "订单ID")
    @TableField(value = "order_id")
    private Long orderId;

    @Schema(description = "综合评分")
    @TableField(value = "overall_rating")
    private BigDecimal overallRating;

    @Schema(description = "专业能力评分")
    @TableField(value = "professional_rating")
    private BigDecimal professionalRating;

    @Schema(description = "服务态度评分")
    @TableField(value = "attitude_rating")
    private BigDecimal attitudeRating;

    @Schema(description = "沟通能力评分")
    @TableField(value = "communication_rating")
    private BigDecimal communicationRating;

    @Schema(description = "评价内容")
    @TableField(value = "content")
    private String content;

    @Schema(description = "评价标签")
    @TableField(value = "tags")
    private String tags;

    @Schema(description = "评价图片JSON")
    @TableField(value = "images")
    private String images;

    @Schema(description = "状态: 0已删除 1正常")
    @TableField(value = "status")
    private Integer status;
}