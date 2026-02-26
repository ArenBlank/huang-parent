package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告
 * @author system
 * @since 2026-02-25
 */
@Schema(description = "公告")
@TableName(value = "notice")
@Data
public class Notice extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "标题")
    @TableField(value = "title")
    private String title;

    @Schema(description = "内容")
    @TableField(value = "content")
    private String content;

    @Schema(description = "状态")
    @TableField(value = "status")
    private Integer status;

    @Schema(description = "发布时间")
    @TableField(value = "publish_time")
    private LocalDateTime publishTime;
}
