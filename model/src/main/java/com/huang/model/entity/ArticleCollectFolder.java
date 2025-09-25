package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文章收藏夹表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "文章收藏夹表")
@TableName(value = "article_collect_folder")
@Data
public class ArticleCollectFolder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "收藏夹名称")
    @TableField(value = "folder_name")
    private String folderName;

    @Schema(description = "描述")
    @TableField(value = "description")
    private String description;

    @Schema(description = "是否公开: 0私密 1公开")
    @TableField(value = "is_public")
    private Integer isPublic;

    @Schema(description = "文章数量")
    @TableField(value = "article_count")
    private Integer articleCount;

    @Schema(description = "排序")
    @TableField(value = "sort_order")
    private Integer sortOrder;
}