package com.huang.web.admin.vo.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教练认证申请列表VO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "教练认证申请列表VO")
@Data
public class CoachCertificationListVO {

    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "专长领域")
    private String specialties;

    @Schema(description = "从业年限")
    private Integer experienceYears;

    @Schema(description = "状态: pending待审核 approved已通过 rejected已拒绝")
    private String status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "申请时间")
    private LocalDateTime applyTime;

    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;

    @Schema(description = "审核人ID")
    private Long reviewerId;

    @Schema(description = "审核人姓名")
    private String reviewerName;

    @Schema(description = "审核备注")
    private String reviewRemark;

    @Schema(description = "认证编号")
    private String certificationNo;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}