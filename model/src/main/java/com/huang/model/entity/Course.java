package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 课程表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "课程表")
@TableName(value = "course")
@Data
public class Course extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "课程名称")
    @TableField(value = "course_name")
    private String courseName;

    @Schema(description = "分类ID")
    @TableField(value = "category_id")
    private Long categoryId;

    @Schema(description = "教练ID")
    @TableField(value = "coach_id")
    private Long coachId;

    @Schema(description = "封面图片")
    @TableField(value = "cover_image")
    private String coverImage;

    @Schema(description = "课程描述")
    @TableField(value = "description")
    private String description;

    @Schema(description = "难度：1-初级，2-中级，3-高级")
    @TableField(value = "difficulty")
    private Integer difficulty;

    @Schema(description = "时长(分钟)")
    @TableField(value = "duration")
    private Integer duration;

    @Schema(description = "最大参与人数")
    @TableField(value = "max_participants")
    private Integer maxParticipants;

    @Schema(description = "价格")
    @TableField(value = "price")
    private BigDecimal price;

    @Schema(description = "状态：0-下架，1-上架")
    @TableField(value = "status")
    private Integer status;
}