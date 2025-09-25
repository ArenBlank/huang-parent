package com.huang.web.app.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 刷新令牌响应VO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "刷新令牌响应")
@Data
public class RefreshTokenVO {

    @Schema(description = "新的访问令牌")
    private String accessToken;

    @Schema(description = "新的刷新令牌（如果需要轮换）")
    private String refreshToken;

    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "访问令牌过期时间")
    private LocalDateTime accessTokenExpire;

    @Schema(description = "刷新令牌过期时间")
    private LocalDateTime refreshTokenExpire;

    @Schema(description = "用户基本信息（如果有更新）")
    private UserInfoVO userInfo;
}