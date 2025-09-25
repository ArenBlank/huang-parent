package com.huang.web.app.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 头像上传请求DTO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "头像上传请求")
@Data
public class AvatarUploadDTO {

    @Schema(description = "头像文件路径（上传完成后的路径）", requiredMode = Schema.RequiredMode.REQUIRED, 
            example = "/uploads/avatar/user_123_avatar.jpg")
    private String avatarUrl;

    @Schema(description = "文件大小（字节）", example = "102400")
    private Long fileSize;

    @Schema(description = "文件类型", example = "image/jpeg")
    private String contentType;

    @Schema(description = "原始文件名", example = "avatar.jpg")
    private String originalFileName;
}