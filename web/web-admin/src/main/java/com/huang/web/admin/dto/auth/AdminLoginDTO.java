package com.huang.web.admin.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Admin login request")
public class AdminLoginDTO {

    @NotBlank
    @Schema(description = "Admin account, username or phone", example = "root_admin")
    private String account;

    @NotBlank
    @Schema(description = "Admin password", example = "root")
    private String password;

    @NotBlank
    @Schema(description = "AJ-Captcha verification token", example = "encrypted-captcha-verification")
    private String captchaVerification;
}
