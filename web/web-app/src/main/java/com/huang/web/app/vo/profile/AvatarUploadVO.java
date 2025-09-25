package com.huang.web.app.vo.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 头像上传响应VO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "头像上传响应")
@Data
public class AvatarUploadVO {

    @Schema(description = "头像访问URL")
    private String avatarUrl;

    @Schema(description = "头像文件名")
    private String fileName;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "上传时间")
    private LocalDateTime uploadTime;

    @Schema(description = "上传状态：success-成功，failed-失败")
    private String status;

    @Schema(description = "状态消息")
    private String message;
}