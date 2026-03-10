package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Course category entity (aligns with course_category table)
 */
@Schema(description = "Course category")
@TableName(value = "course_category")
@Data
public class CourseCategory extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Category name")
    @TableField(value = "name")
    private String name;

    @Schema(description = "Sort order")
    @TableField(value = "sort")
    private Integer sort;

    @Schema(description = "Status: 0-disabled, 1-enabled")
    @TableField(value = "status")
    private Integer status;
}
