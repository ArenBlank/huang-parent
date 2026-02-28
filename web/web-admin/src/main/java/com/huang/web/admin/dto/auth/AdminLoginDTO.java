package com.huang.web.admin.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminLoginDTO {

    @NotBlank
    private String account;

    @NotBlank
    private String password;
}

