package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 教练信息表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "教练信息表")
@TableName(value = "coach")
@Data
public class Coach extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "真实姓名")
    @TableField(value = "real_name")
    private String realName;

    @Schema(description = "认证编号")
    @TableField(value = "certification_no")
    private String certificationNo;

    @Schema(description = "专长领域")
    @TableField(value = "specialties")
    private String specialties;

    @Schema(description = "个人介绍")
    @TableField(value = "introduction")
    private String introduction;

    @Schema(description = "从业年限")
    @TableField(value = "experience_years")
    private Integer experienceYears;

    @Schema(description = "评分")
    @TableField(value = "rating")
    private BigDecimal rating;

    @Schema(description = "状态：0-离职，1-在职，2-休假")
    @TableField(value = "status")
    private Integer status;
}