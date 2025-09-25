package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 健康科普文章表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "健康科普文章表")
@TableName(value = "health_article")
@Data
public class HealthArticle extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文章编号")
    @TableField(value = "article_no")
    private String articleNo;

    @Schema(description = "文章标题")
    @TableField(value = "title")
    private String title;

    @Schema(description = "副标题")
    @TableField(value = "subtitle")
    private String subtitle;

    @Schema(description = "作者ID（教练用户ID）")
    @TableField(value = "author_id")
    private Long authorId;

    @Schema(description = "作者名称")
    @TableField(value = "author_name")
    private String authorName;

    @Schema(description = "分类ID")
    @TableField(value = "category_id")
    private Long categoryId;

    @Schema(description = "封面图片")
    @TableField(value = "cover_image")
    private String coverImage;

    @Schema(description = "文章摘要")
    @TableField(value = "summary")
    private String summary;

    @Schema(description = "文章内容（富文本）")
    @TableField(value = "content")
    private String content;

    @Schema(description = "内容类型: richtext富文本 markdown")
    @TableField(value = "content_type")
    private String contentType;

    @Schema(description = "标签JSON数组")
    @TableField(value = "tags")
    private String tags;

    @Schema(description = "SEO关键词")
    @TableField(value = "keywords")
    private String keywords;

    @Schema(description = "文章来源")
    @TableField(value = "source")
    private String source;

    @Schema(description = "预计阅读时长(分钟)")
    @TableField(value = "reading_time")
    private Integer readingTime;

    @Schema(description = "难度等级: beginner初级 intermediate中级 advanced高级")
    @TableField(value = "difficulty_level")
    private String difficultyLevel;

    @Schema(description = "状态: draft草稿 pending待审核 approved已通过 rejected已拒绝 published已发布 offline已下线")
    @TableField(value = "status")
    private String status;

    @Schema(description = "提交审核时间")
    @TableField(value = "submit_time")
    private LocalDateTime submitTime;

    @Schema(description = "审核时间")
    @TableField(value = "audit_time")
    private LocalDateTime auditTime;

    @Schema(description = "审核人ID")
    @TableField(value = "audit_by")
    private Long auditBy;

    @Schema(description = "审核备注")
    @TableField(value = "audit_remark")
    private String auditRemark;

    @Schema(description = "拒绝原因")
    @TableField(value = "reject_reason")
    private String rejectReason;

    @Schema(description = "发布时间")
    @TableField(value = "publish_time")
    private LocalDateTime publishTime;

    @Schema(description = "浏览次数")
    @TableField(value = "view_count")
    private Integer viewCount;

    @Schema(description = "点赞数")
    @TableField(value = "like_count")
    private Integer likeCount;

    @Schema(description = "收藏数")
    @TableField(value = "collect_count")
    private Integer collectCount;

    @Schema(description = "分享数")
    @TableField(value = "share_count")
    private Integer shareCount;

    @Schema(description = "评论数")
    @TableField(value = "comment_count")
    private Integer commentCount;

    @Schema(description = "是否置顶: 0否 1是")
    @TableField(value = "is_top")
    private Integer isTop;

    @Schema(description = "是否推荐: 0否 1是")
    @TableField(value = "is_recommend")
    private Integer isRecommend;

    @Schema(description = "是否热门: 0否 1是")
    @TableField(value = "is_hot")
    private Integer isHot;

    @Schema(description = "排序权重")
    @TableField(value = "sort_order")
    private Integer sortOrder;

    @Schema(description = "是否允许评论: 0否 1是")
    @TableField(value = "allow_comment")
    private Integer allowComment;
}
