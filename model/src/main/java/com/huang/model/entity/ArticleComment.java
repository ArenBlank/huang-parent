package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文章评论表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "文章评论表")
@TableName(value = "article_comment")
@Data
public class ArticleComment extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文章ID")
    @TableField(value = "article_id")
    private Long articleId;

    @Schema(description = "评论用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "父评论ID（0为顶级评论）")
    @TableField(value = "parent_id")
    private Long parentId;

    @Schema(description = "评论内容")
    @TableField(value = "content")
    private String content;

    @Schema(description = "评论图片")
    @TableField(value = "images")
    private String images;

    @Schema(description = "点赞数")
    @TableField(value = "like_count")
    private Integer likeCount;

    @Schema(description = "回复数")
    @TableField(value = "reply_count")
    private Integer replyCount;

    @Schema(description = "是否作者回复: 0否 1是")
    @TableField(value = "is_author")
    private Integer isAuthor;

    @Schema(description = "状态: 0已删除 1正常 2已屏蔽")
    @TableField(value = "status")
    private Integer status;
}