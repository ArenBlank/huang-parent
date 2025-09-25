package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 教练服务项目表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "教练服务项目表")
@TableName(value = "coach_service")
@Data
public class CoachService extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "教练ID")
    @TableField(value = "coach_id")
    private Long coachId;

    @Schema(description = "服务名称")
    @TableField(value = "service_name")
    private String serviceName;

    @Schema(description = "服务类型: consultation咨询 training私教 assessment评估")
    @TableField(value = "service_type")
    private String serviceType;

    @Schema(description = "服务描述")
    @TableField(value = "description")
    private String description;

    @Schema(description = "服务时长(分钟)")
    @TableField(value = "duration")
    private Integer duration;

    @Schema(description = "价格")
    @TableField(value = "price")
    private BigDecimal price;

    @Schema(description = "最大服务人数")
    @TableField(value = "max_clients")
    private Integer maxClients;

    @Schema(description = "状态: 0停用 1启用")
    @TableField(value = "status")
    private Integer status;
}