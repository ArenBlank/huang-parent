package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 健康档案表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "健康档案表")
@TableName(value = "health_record")
@Data
public class HealthRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "身高(cm)")
    @TableField(value = "height")
    private BigDecimal height;

    @Schema(description = "体重(kg)")
    @TableField(value = "weight")
    private BigDecimal weight;

    @Schema(description = "BMI指数")
    @TableField(value = "bmi")
    private BigDecimal bmi;

    @Schema(description = "体脂率(%)")
    @TableField(value = "body_fat_rate")
    private BigDecimal bodyFatRate;

    @Schema(description = "肌肉率(%)")
    @TableField(value = "muscle_rate")
    private BigDecimal muscleRate;

    @Schema(description = "基础代谢率")
    @TableField(value = "basal_metabolism")
    private Integer basalMetabolism;

    @Schema(description = "健康目标")
    @TableField(value = "health_goal")
    private String healthGoal;

    @Schema(description = "病史")
    @TableField(value = "medical_history")
    private String medicalHistory;

    @Schema(description = "过敏信息")
    @TableField(value = "allergies")
    private String allergies;
}