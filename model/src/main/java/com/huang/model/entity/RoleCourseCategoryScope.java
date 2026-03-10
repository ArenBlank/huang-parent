package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "角色课程分类范围")
@TableName(value = "role_course_category_scope")
@Data
public class RoleCourseCategoryScope extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID")
    @TableField(value = "role_id")
    private Long roleId;

    @Schema(description = "课程分类ID")
    @TableField(value = "category_id")
    private Long categoryId;
}
