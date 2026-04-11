package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "Task run log")
@TableName("task_run_log")
public class TaskRunLog implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "Primary key")
    private Long id;

    @TableField("task_code")
    @Schema(description = "Task code", example = "PAYMENT_COMPENSATE")
    private String taskCode;

    @TableField("task_name")
    @Schema(description = "Task name", example = "Payment compensate")
    private String taskName;

    @TableField("trigger_mode")
    @Schema(description = "Trigger mode", example = "MANUAL")
    private String triggerMode;

    @TableField("run_status")
    @Schema(description = "Run status", example = "SUCCESS")
    private String runStatus;

    @TableField("instance_id")
    @Schema(description = "Instance identifier", example = "web-admin:8080")
    private String instanceId;

    @TableField("started_at")
    @Schema(description = "Started at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    @Schema(description = "Finished at")
    private LocalDateTime finishedAt;

    @TableField("duration_ms")
    @Schema(description = "Duration in milliseconds", example = "128")
    private Long durationMs;

    @TableField("affected_count")
    @Schema(description = "Affected row count", example = "3")
    private Integer affectedCount;

    @TableField("message")
    @Schema(description = "Run message", example = "ok")
    private String message;

    @TableField("created_at")
    @Schema(description = "Created at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @Schema(description = "Updated at")
    private LocalDateTime updatedAt;
}
