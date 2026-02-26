package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 训练打卡记录
 * @author system
 * @since 2026-02-25
 */
@Schema(description = "训练打卡记录")
@TableName(value = "training_record")
@Data
public class TrainingRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "计划ID")
    @TableField(value = "plan_id")
    private Long planId;

    @Schema(description = "计划明细ID")
    @TableField(value = "plan_item_id")
    private Long planItemId;

    @Schema(description = "记录日期")
    @TableField(value = "record_date")
    private LocalDate recordDate;

    @Schema(description = "时长(分钟)")
    @TableField(value = "duration_min")
    private Integer durationMin;

    @Schema(description = "热量")
    @TableField(value = "calories")
    private Integer calories;

    @Schema(description = "体感")
    @TableField(value = "feeling")
    private String feeling;

    @Schema(description = "打卡图片")
    @TableField(value = "record_images")
    private String recordImages;
}
