package com.huang.web.app.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 短信验证码请求DTO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "短信验证码请求")
@Data
public class SmsCodeDTO {

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13888888888")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "验证码类型：register-注册，login-登录，reset_password-重置密码", 
            requiredMode = Schema.RequiredMode.REQUIRED, example = "register")
    @NotBlank(message = "验证码类型不能为空")
    @Pattern(regexp = "^(register|login|reset_password)$", message = "验证码类型只能为register、login或reset_password")
    private String type;

    @Schema(description = "图形验证码（如果开启图形验证码校验）", example = "abcd")
    private String captcha;

    @Schema(description = "图形验证码key（如果开启图形验证码校验）", example = "captcha_key_123")
    private String captchaKey;
}