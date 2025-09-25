package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 运动计划表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "运动计划表")
@TableName(value = "exercise_plan")
@Data
public class ExercisePlan extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "计划名称")
    @TableField(value = "plan_name")
    private String planName;

    @Schema(description = "目标")
    @TableField(value = "goal")
    private String goal;

    @Schema(description = "开始日期")
    @TableField(value = "start_date")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    @TableField(value = "end_date")
    private LocalDate endDate;

    @Schema(description = "计划详情(JSON)")
    @TableField(value = "plan_details")
    private String planDetails;

    @Schema(description = "状态：0-已取消，1-进行中，2-已完成")
    @TableField(value = "status")
    private Integer status;
}