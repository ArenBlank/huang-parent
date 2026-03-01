package com.huang.web.admin.vo.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "管理端教练申请列表项")
public class AdminCoachApplyVO {

    private Long profileId;
    private Long userId;
    private String username;
    private String nickname;
    private String phone;
    private String bio;
    private String expertise;
    private Integer years;
    private BigDecimal price;
    private Integer certStatus;
    private String certStatusText;
    private Integer status;
    private String updateTime;
}

