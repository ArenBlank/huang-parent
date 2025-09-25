package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 课程分类表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "课程分类表")
@TableName(value = "course_category")
@Data
public class CourseCategory extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分类名称")
    @TableField(value = "category_name")
    private String categoryName;

    @Schema(description = "父分类ID")
    @TableField(value = "parent_id")
    private Long parentId;

    @Schema(description = "排序")
    @TableField(value = "sort_order")
    private Integer sortOrder;

    @Schema(description = "状态：0-禁用，1-正常")
    @TableField(value = "status")
    private Integer status;
}