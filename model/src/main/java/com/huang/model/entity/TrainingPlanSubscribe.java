package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 训练计划订阅
 * @author system
 * @since 2026-02-25
 */
@Schema(description = "训练计划订阅")
@TableName(value = "training_plan_subscribe")
@Data
public class TrainingPlanSubscribe extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "计划ID")
    @TableField(value = "plan_id")
    private Long planId;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "开始日期")
    @TableField(value = "start_date")
    private LocalDate startDate;

    @Schema(description = "状态")
    @TableField(value = "status")
    private Integer status;
}
