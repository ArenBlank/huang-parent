package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教练认证申请表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "教练认证申请表")
@TableName(value = "coach_certification_apply")
@Data
public class CoachCertificationApply extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "真实姓名")
    @TableField(value = "real_name")
    private String realName;

    @Schema(description = "身份证号")
    @TableField(value = "id_card")
    private String idCard;

    @Schema(description = "联系电话")
    @TableField(value = "phone")
    private String phone;

    @Schema(description = "邮箱")
    @TableField(value = "email")
    private String email;

    @Schema(description = "专长领域")
    @TableField(value = "specialties")
    private String specialties;

    @Schema(description = "从业年限")
    @TableField(value = "experience_years")
    private Integer experienceYears;

    @Schema(description = "个人介绍")
    @TableField(value = "introduction")
    private String introduction;

    @Schema(description = "证书信息JSON")
    @TableField(value = "certificates")
    private String certificates;

    @Schema(description = "教育背景JSON")
    @TableField(value = "education")
    private String education;

    @Schema(description = "工作经历JSON")
    @TableField(value = "work_experience")
    private String workExperience;

    @Schema(description = "状态: pending待审核 approved已通过 rejected已拒绝")
    @TableField(value = "status")
    private String status;

    @Schema(description = "申请时间")
    @TableField(value = "apply_time")
    private LocalDateTime applyTime;

    @Schema(description = "审核时间")
    @TableField(value = "review_time")
    private LocalDateTime reviewTime;

    @Schema(description = "审核人ID")
    @TableField(value = "reviewer_id")
    private Long reviewerId;

    @Schema(description = "审核备注")
    @TableField(value = "review_remark")
    private String reviewRemark;

    @Schema(description = "认证编号(审核通过后生成)")
    @TableField(value = "certification_no")
    private String certificationNo;
}