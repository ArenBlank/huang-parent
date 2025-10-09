package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教练咨询记录表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "教练咨询记录表")
@TableName(value = "coach_consultation")
@Data
public class CoachConsultation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "教练ID")
    @TableField(value = "coach_id")
    private Long coachId;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "咨询类型: online在线 offline线下")
    @TableField(value = "consultation_type")
    private String consultationType;

    @Schema(description = "咨询时间")
    @TableField(value = "consultation_date")
    private LocalDateTime consultationDate;

    @Schema(description = "咨询时长(分钟)")
    @TableField(value = "duration")
    private Integer duration;

    @Schema(description = "咨询主题")
    @TableField(value = "topic")
    private String topic;

    @Schema(description = "咨询内容")
    @TableField(value = "content")
    private String content;

    @Schema(description = "教练建议")
    @TableField(value = "coach_advice")
    private String coachAdvice;

    @Schema(description = "状态: scheduled已预约 completed已完成 cancelled已取消")
    @TableField(value = "status")
    private String status;
}