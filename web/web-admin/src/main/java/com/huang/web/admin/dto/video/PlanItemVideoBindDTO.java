package com.huang.web.admin.dto.video;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "训练计划项绑定视频请求")
public class PlanItemVideoBindDTO {

    @Schema(description = "计划项ID", required = true)
    @NotNull(message = "计划项ID不能为空")
    private Long planItemId;

    @Schema(description = "视频ID", required = true)
    @NotNull(message = "视频ID不能为空")
    private Long videoId;
}
