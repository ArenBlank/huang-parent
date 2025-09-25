package com.huang.web.app.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录响应VO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "登录响应")
@Data
public class LoginVO {

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "访问令牌过期时间")
    private LocalDateTime accessTokenExpire;

    @Schema(description = "刷新令牌过期时间")
    private LocalDateTime refreshTokenExpire;

    @Schema(description = "用户信息")
    private UserInfoVO userInfo;

    @Schema(description = "是否首次登录")
    private Boolean firstLogin;

    @Schema(description = "是否需要完善资料")
    private Boolean needCompleteProfile;
}