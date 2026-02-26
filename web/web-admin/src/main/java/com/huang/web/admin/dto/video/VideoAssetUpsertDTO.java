package com.huang.web.admin.dto.video;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "视频素材新增/更新请求")
public class VideoAssetUpsertDTO {

    @Schema(description = "视频标题", required = true)
    @NotBlank(message = "视频标题不能为空")
    private String title;

    @Schema(description = "来源站点", required = true, example = "pexels")
    @NotBlank(message = "来源站点不能为空")
    private String sourceSite;

    @Schema(description = "来源链接", required = true)
    @NotBlank(message = "来源链接不能为空")
    private String sourceUrl;

    @Schema(description = "授权类型", required = true, example = "Pexels License")
    @NotBlank(message = "授权类型不能为空")
    private String licenseType;

    @Schema(description = "是否要求署名: 0否 1是", required = true)
    @NotNull(message = "署名要求不能为空")
    private Integer attributionRequired;

    @Schema(description = "作者名称")
    private String authorName;

    @Schema(description = "视频时长(秒)")
    private Integer durationSec;

    @Schema(description = "标签，逗号分隔")
    private String tags;

    @Schema(description = "MinIO对象路径", required = true, example = "videos/pexels/squat-demo.mp4")
    @NotBlank(message = "MinIO路径不能为空")
    private String minioPath;

    @Schema(description = "状态: 0停用 1启用", required = true)
    @NotNull(message = "状态不能为空")
    private Integer status;
}
