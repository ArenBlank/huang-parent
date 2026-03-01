package com.huang.web.app.vo.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "我的教练申请")
public class MyCoachApplyVO {

    private Long profileId;
    private String bio;
    private String expertise;
    private Integer years;
    private BigDecimal price;
    private Integer certStatus;
    private String certStatusText;
    private Integer status;
    private String updateTime;
}

