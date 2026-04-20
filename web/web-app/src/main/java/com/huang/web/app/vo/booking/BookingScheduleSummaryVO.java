package com.huang.web.app.vo.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "预约档期摘要")
@Data
public class BookingScheduleSummaryVO {

    @Schema(description = "筛选的教练ID")
    private Long coachId;

    @Schema(description = "当前有效档期总数")
    private Long totalSchedules;

    @Schema(description = "未来档期数")
    private Long futureSchedules;

    @Schema(description = "未来可预约档期数")
    private Long availableSchedules;

    @Schema(description = "最近一条档期日期")
    private LocalDate lastScheduleDate;

    @Schema(description = "下一条未来档期日期")
    private LocalDate nextScheduleDate;
}
