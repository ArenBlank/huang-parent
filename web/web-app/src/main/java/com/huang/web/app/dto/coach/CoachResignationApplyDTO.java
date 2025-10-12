package com.huang.web.app.dto.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * App端教练离职申请DTO
 * @author system
 * @since 2025-01-25
 */
@Schema(description = "教练离职申请DTO")
@Data
public class CoachResignationApplyDTO {

    @Schema(description = "预计离职日期", required = true, example = "2025-02-01")
    @NotNull(message = "预计离职日期不能为空")
    @Future(message = "预计离职日期必须是未来日期")
    private LocalDate resignationDate;

    @Schema(description = "离职原因", required = true)
    @NotBlank(message = "离职原因不能为空")
    @Size(max = 500, message = "离职原因不能超过500字")
    private String reason;

    @Schema(description = "交接计划", example = "将课程交接给张教练，预计3天完成")
    @Size(max = 1000, message = "交接计划不能超过1000字")
    private String handoverPlan;
}
