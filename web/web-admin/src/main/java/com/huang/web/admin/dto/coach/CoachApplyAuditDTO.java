package com.huang.web.admin.dto.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "教练申请审核")
public class CoachApplyAuditDTO {

    @NotNull
    @Schema(description = "申请资料ID")
    private Long profileId;

    @NotNull
    @Schema(description = "审核结果: 1通过 2驳回")
    private Integer certStatus;
}

