package com.huang.web.app.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 用户登录请求DTO
 * @author system
 * @since 2026-02-25
 */
@Schema(description = "用户登录请求")
@Data
public class UserLoginDTO {

    @Schema(description = "登录账号（用户名/手机号）", requiredMode = Schema.RequiredMode.REQUIRED, example = "13888888888")
    @NotBlank(message = "登录账号不能为空")
    private String account;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "登录类型：password-密码登录", requiredMode = Schema.RequiredMode.REQUIRED, example = "password")
    @NotBlank(message = "登录类型不能为空")
    @Pattern(regexp = "^password$", message = "登录类型只能为password")
    private String loginType;

    @Schema(description = "短信验证码（已弃用）", example = "123456")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须为6位数字")
    private String smsCode;

    @Schema(description = "滑块拼图二次校验token", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "滑块验证不能为空")
    private String captchaVerification;

    @Schema(description = "设备标识", example = "device_123456")
    private String deviceId;

    @Schema(description = "设备类型：android、ios、web", example = "android")
    @Pattern(regexp = "^(android|ios|web)$", message = "设备类型只能为android、ios或web")
    private String deviceType;
}
