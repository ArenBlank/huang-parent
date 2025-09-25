package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文章点赞表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "文章点赞表")
@TableName(value = "article_like")
@Data
public class ArticleLike extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文章ID")
    @TableField(value = "article_id")
    private Long articleId;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;
}