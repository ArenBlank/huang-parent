package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 视频资产
 * @author system
 * @since 2026-02-25
 */
@Schema(description = "视频资产")
@TableName(value = "video_asset")
@Data
public class VideoAsset extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "标题")
    @TableField(value = "title")
    private String title;

    @Schema(description = "来源站点")
    @TableField(value = "source_site")
    private String sourceSite;

    @Schema(description = "来源链接")
    @TableField(value = "source_url")
    private String sourceUrl;

    @Schema(description = "许可类型")
    @TableField(value = "license_type")
    private String licenseType;

    @Schema(description = "是否需要署名")
    @TableField(value = "attribution_required")
    private Integer attributionRequired;

    @Schema(description = "作者")
    @TableField(value = "author_name")
    private String authorName;

    @Schema(description = "时长(秒)")
    @TableField(value = "duration_sec")
    private Integer durationSec;

    @Schema(description = "标签")
    @TableField(value = "tags")
    private String tags;

    @Schema(description = "MinIO路径")
    @TableField(value = "minio_path")
    private String minioPath;

    @Schema(description = "状态")
    @TableField(value = "status")
    private Integer status;
}
