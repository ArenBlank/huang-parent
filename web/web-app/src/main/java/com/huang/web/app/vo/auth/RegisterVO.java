package com.huang.web.app.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 注册响应VO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "注册响应")
@Data
public class RegisterVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "注册时间")
    private LocalDateTime registerTime;

    @Schema(description = "是否自动登录")
    private Boolean autoLogin;

    @Schema(description = "访问令牌（自动登录时返回）")
    private String accessToken;

    @Schema(description = "刷新令牌（自动登录时返回）")
    private String refreshToken;

    @Schema(description = "欢迎消息")
    private String welcomeMessage;
}