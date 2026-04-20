package com.huang.web.app.vo.course;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Schema(description = "课程报名历史")
@Data
public class CourseEnrollmentHistoryVO {

    @Schema(description = "报名ID")
    private Long id;

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "课程标题")
    private String courseTitle;

    @Schema(description = "排期ID")
    private Long scheduleId;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "交易状态")
    private Integer status;

    @Schema(description = "履约状态")
    private Integer attendStatus;

    @Schema(description = "报名时间")
    private LocalDateTime enrollTime;

    @Schema(description = "创建时间")
    private Date createTime;
}
