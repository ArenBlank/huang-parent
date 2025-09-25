package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 运动记录表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "运动记录表")
@TableName(value = "exercise_record")
@Data
public class ExerciseRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "运动日期")
    @TableField(value = "exercise_date")
    private LocalDate exerciseDate;

    @Schema(description = "运动类型")
    @TableField(value = "exercise_type")
    private String exerciseType;

    @Schema(description = "时长(分钟)")
    @TableField(value = "duration")
    private Integer duration;

    @Schema(description = "消耗卡路里")
    @TableField(value = "calories")
    private Integer calories;

    @Schema(description = "距离(公里)")
    @TableField(value = "distance")
    private BigDecimal distance;

    @Schema(description = "步数")
    @TableField(value = "steps")
    private Integer steps;

    @Schema(description = "平均心率")
    @TableField(value = "heart_rate_avg")
    private Integer heartRateAvg;

    @Schema(description = "备注")
    @TableField(value = "notes")
    private String notes;
}
