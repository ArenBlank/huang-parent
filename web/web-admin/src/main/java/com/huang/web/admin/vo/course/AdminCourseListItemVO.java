package com.huang.web.admin.vo.course;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminCourseListItemVO {

    private Long id;
    private Long categoryId;
    private String title;
    private String summary;
    private String coverUrl;
    private String level;
    private Integer durationMin;
    private BigDecimal price;
    private Integer status;

    private String lifecycleStatus;
    private String lifecycleLabel;
    private Integer totalSchedules;
    private Integer activeSchedules;
    private Boolean hasEnrollment;
    private LocalDateTime latestStartTime;
    private LocalDateTime latestEndTime;
}
