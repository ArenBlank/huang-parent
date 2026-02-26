package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 训练计划明细
 * @author system
 * @since 2026-02-25
 */
@Schema(description = "训练计划明细")
@TableName(value = "training_plan_item")
@Data
public class TrainingPlanItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "计划ID")
    @TableField(value = "plan_id")
    private Long planId;

    @Schema(description = "第几天")
    @TableField(value = "day_index")
    private Integer dayIndex;

    @Schema(description = "动作名称")
    @TableField(value = "action_name")
    private String actionName;

    @Schema(description = "组数")
    @TableField(value = "`sets`")
    private Integer sets;

    @Schema(description = "次数")
    @TableField(value = "reps")
    private Integer reps;

    @Schema(description = "时长(分钟)")
    @TableField(value = "duration_min")
    private Integer durationMin;

    @Schema(description = "休息时间(秒)")
    @TableField(value = "rest_sec")
    private Integer restSec;

    @Schema(description = "视频ID")
    @TableField(value = "video_id")
    private Long videoId;

    @Schema(description = "排序")
    @TableField(value = "`sort`")
    private Integer sort;
}
