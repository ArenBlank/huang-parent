package com.huang.web.admin.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Admin refresh token response")
public class AdminRefreshTokenVO {

    @Schema(description = "新的访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "访问令牌过期时间")
    private LocalDateTime accessTokenExpire;

    @Schema(description = "刷新令牌过期时间")
    private LocalDateTime refreshTokenExpire;
}
