package com.huang.web.admin.vo.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "管理端教练档期可选教练")
public class AdminCoachOptionVO {

    @Schema(description = "教练档案ID")
    private Long coachId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "显示名称")
    private String displayName;

    @Schema(description = "账号")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "擅长领域")
    private String expertise;

    @Schema(description = "教龄")
    private Integer years;

    @Schema(description = "基础价格")
    private BigDecimal price;
}
