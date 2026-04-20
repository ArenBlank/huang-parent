package com.huang.web.app.vo.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

@Schema(description = "我的预约历史")
@Data
public class BookingHistoryVO {

    @Schema(description = "预约ID")
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单金额")
    private BigDecimal amount;

    @Schema(description = "教练ID")
    private Long coachId;

    @Schema(description = "档期ID")
    private Long scheduleId;

    @Schema(description = "排期日期")
    private LocalDate scheduleDate;

    @Schema(description = "开始时间")
    private LocalTime startTime;

    @Schema(description = "结束时间")
    private LocalTime endTime;

    @Schema(description = "预约状态")
    private String bookingStatus;

    @Schema(description = "支付状态")
    private String payStatus;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "完成时间")
    private java.time.LocalDateTime finishTime;
}
