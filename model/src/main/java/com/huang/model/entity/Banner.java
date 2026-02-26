package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 轮播图
 * @author system
 * @since 2026-02-25
 */
@Schema(description = "轮播图")
@TableName(value = "banner")
@Data
public class Banner extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "标题")
    @TableField(value = "title")
    private String title;

    @Schema(description = "图片URL")
    @TableField(value = "image_url")
    private String imageUrl;

    @Schema(description = "链接URL")
    @TableField(value = "link_url")
    private String linkUrl;

    @Schema(description = "排序")
    @TableField(value = "sort")
    private Integer sort;

    @Schema(description = "状态")
    @TableField(value = "status")
    private Integer status;
}
