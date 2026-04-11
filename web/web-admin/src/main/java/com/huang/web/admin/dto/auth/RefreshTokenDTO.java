package com.huang.web.admin.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Admin refresh token request")
public class RefreshTokenDTO {

    @NotBlank(message = "refresh token must not be blank")
    @Schema(description = "Refresh token", requiredMode = Schema.RequiredMode.REQUIRED, example = "eyJhbGciOiJIUzI1NiJ9.admin-refresh-token")
    private String refreshToken;
}
