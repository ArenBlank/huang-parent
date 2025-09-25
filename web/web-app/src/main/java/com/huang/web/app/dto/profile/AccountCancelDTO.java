package com.huang.web.app.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 账号注销请求DTO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "账号注销请求")
@Data
public class AccountCancelDTO {

    @Schema(description = "注销原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "不再使用")
    @NotBlank(message = "注销原因不能为空")
    private String reason;

    @Schema(description = "注销类型：temporary-临时注销，permanent-永久注销", 
            requiredMode = Schema.RequiredMode.REQUIRED, example = "temporary")
    @NotBlank(message = "注销类型不能为空")
    @Pattern(regexp = "^(temporary|permanent)$", message = "注销类型只能为temporary或permanent")
    private String cancelType;

    @Schema(description = "密码确认", requiredMode = Schema.RequiredMode.REQUIRED, example = "password123")
    @NotBlank(message = "密码确认不能为空")
    private String password;

    @Schema(description = "短信验证码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "短信验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须为6位数字")
    private String smsCode;

    @Schema(description = "附加说明", example = "其他详细说明")
    private String additionalNote;
}