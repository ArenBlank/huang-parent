package com.huang.web.app.vo.course;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "我的待上课行程")
@Data
public class CourseMyScheduleVO {

    @Schema(description = "报名ID")
    private Long enrollmentId;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "课程标题")
    private String courseTitle;

    @Schema(description = "课程封面")
    private String coverUrl;

    @Schema(description = "排期ID")
    private Long scheduleId;

    @Schema(description = "教练ID")
    private Long coachId;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "价格")
    private BigDecimal price;

    @Schema(description = "核销码")
    private String checkInCode;

    @Schema(description = "履约状态")
    private Integer attendStatus;
}
