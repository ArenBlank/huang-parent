package com.huang.web.admin.dto.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Schema(description = "教练档期新增/编辑请求")
public class CoachScheduleUpsertDTO {

    @NotNull(message = "教练ID不能为空")
    @Schema(description = "教练档案ID")
    private Long coachId;

    @NotNull(message = "日期不能为空")
    @Schema(description = "档期日期")
    private LocalDate scheduleDate;

    @NotNull(message = "开始时间不能为空")
    @Schema(description = "开始时间")
    private LocalTime startTime;

    @NotNull(message = "结束时间不能为空")
    @Schema(description = "结束时间")
    private LocalTime endTime;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0", message = "价格不能小于0")
    @Schema(description = "价格")
    private BigDecimal price;

    @NotNull(message = "容量不能为空")
    @Min(value = 1, message = "容量至少为1")
    @Schema(description = "容量")
    private Integer capacity;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态，0停用，1启用")
    private Integer status;
}
