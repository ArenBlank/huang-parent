package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 教练评价
 * @author system
 * @since 2026-02-25
 */
@Schema(description = "教练评价")
@TableName(value = "coach_review")
@Data
public class CoachReview extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "预约ID")
    @TableField(value = "booking_id")
    private Long bookingId;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "教练ID")
    @TableField(value = "coach_id")
    private Long coachId;

    @Schema(description = "评分")
    @TableField(value = "score")
    private Integer score;

    @Schema(description = "内容")
    @TableField(value = "content")
    private String content;
}
