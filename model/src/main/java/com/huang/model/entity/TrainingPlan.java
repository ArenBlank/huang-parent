package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 训练计划
 * @author system
 * @since 2026-02-25
 */
@Schema(description = "训练计划")
@TableName(value = "training_plan")
@Data
public class TrainingPlan extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "标题")
    @TableField(value = "title")
    private String title;

    @Schema(description = "目标")
    @TableField(value = "goal")
    private String goal;

    @Schema(description = "难度")
    @TableField(value = "level")
    private String level;

    @Schema(description = "周期(周)")
    @TableField(value = "duration_weeks")
    private Integer durationWeeks;

    @Schema(description = "封面")
    @TableField(value = "cover_url")
    private String coverUrl;

    @Schema(description = "状态")
    @TableField(value = "status")
    private Integer status;
}
