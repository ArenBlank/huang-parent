package com.huang.web.app.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新令牌请求DTO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "刷新令牌请求")
@Data
public class RefreshTokenDTO {

    @Schema(description = "刷新令牌", requiredMode = Schema.RequiredMode.REQUIRED, example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;

    @Schema(description = "设备标识", example = "device_123456")
    private String deviceId;

    @Schema(description = "设备类型：android、ios、web", example = "android")
    private String deviceType;
}