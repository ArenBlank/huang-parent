package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 教练门店关联表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "教练门店关联表")
@TableName(value = "coach_store_relation")
@Data
public class CoachStoreRelation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "教练ID")
    @TableField(value = "coach_id")
    private Long coachId;

    @Schema(description = "门店ID")
    @TableField(value = "store_id")
    private Long storeId;

    @Schema(description = "是否主要工作门店：0-否，1-是")
    @TableField(value = "is_primary")
    private Integer isPrimary;

    @Schema(description = "开始工作日期")
    @TableField(value = "start_date")
    private LocalDate startDate;

    @Schema(description = "结束工作日期")
    @TableField(value = "end_date")
    private LocalDate endDate;

    @Schema(description = "状态：0-停用，1-启用")
    @TableField(value = "status")
    private Integer status;
}