package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文章分类表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "文章分类表")
@TableName(value = "article_category")
@Data
public class ArticleCategory extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "父分类ID")
    @TableField(value = "parent_id")
    private Long parentId;

    @Schema(description = "分类名称")
    @TableField(value = "category_name")
    private String categoryName;

    @Schema(description = "分类编码")
    @TableField(value = "category_code")
    private String categoryCode;

    @Schema(description = "分类图标")
    @TableField(value = "icon")
    private String icon;

    @Schema(description = "分类描述")
    @TableField(value = "description")
    private String description;

    @Schema(description = "排序")
    @TableField(value = "sort_order")
    private Integer sortOrder;

    @Schema(description = "状态: 0禁用 1启用")
    @TableField(value = "status")
    private Integer status;
}
