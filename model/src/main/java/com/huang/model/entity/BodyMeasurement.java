package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 体测记录表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "体测记录表")
@TableName(value = "body_measurement")
@Data
public class BodyMeasurement extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "测量日期")
    @TableField(value = "measure_date")
    private LocalDate measureDate;

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

    @Schema(description = "肌肉量(kg)")
    @TableField(value = "muscle_mass")
    private BigDecimal muscleMass;

    @Schema(description = "内脏脂肪等级")
    @TableField(value = "visceral_fat")
    private Integer visceralFat;

    @Schema(description = "腰围(cm)")
    @TableField(value = "waist")
    private BigDecimal waist;

    @Schema(description = "臀围(cm)")
    @TableField(value = "hip")
    private BigDecimal hip;

    @Schema(description = "胸围(cm)")
    @TableField(value = "chest")
    private BigDecimal chest;

    @Schema(description = "臂围(cm)")
    @TableField(value = "arm")
    private BigDecimal arm;

    @Schema(description = "大腿围(cm)")
    @TableField(value = "thigh")
    private BigDecimal thigh;
}