package com.huang.web.app.vo.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教练认证申请VO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "教练认证申请响应")
@Data
public class CoachCertificationApplyVO {

    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "身份证号")
    private String idCard;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "专长领域")
    private String specialties;

    @Schema(description = "从业年限")
    private Integer experienceYears;

    @Schema(description = "个人介绍")
    private String introduction;

    @Schema(description = "申请状态: pending待审核 approved已通过 rejected已拒绝 cancelled已取消")
    private String status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "申请时间")
    private LocalDateTime applyTime;

    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;

    @Schema(description = "审核人ID")
    private Long reviewerId;

    @Schema(description = "审核备注")
    private String reviewRemark;

    @Schema(description = "认证编号(审核通过后生成)")
    private String certificationNo;

    @Schema(description = "是否可以取消申请")
    private Boolean canCancel;

    @Schema(description = "是否可以重新申请")
    private Boolean canReapply;

    @Schema(description = "证书信息列表")
    private List<CertificateInfo> certificates;

    @Schema(description = "教育背景列表")
    private List<EducationInfo> education;

    @Schema(description = "工作经历列表")
    private List<WorkExperience> workExperience;

    @Schema(description = "证书信息")
    @Data
    public static class CertificateInfo {
        @Schema(description = "证书名称")
        private String name;
        
        @Schema(description = "颁发机构")
        private String issuer;
        
        @Schema(description = "获得时间")
        private String issueDate;
        
        @Schema(description = "证书编号")
        private String certificateNo;
        
        @Schema(description = "证书图片URL")
        private String imageUrl;
    }

    @Schema(description = "教育背景")
    @Data
    public static class EducationInfo {
        @Schema(description = "学校名称")
        private String school;
        
        @Schema(description = "专业名称")
        private String major;
        
        @Schema(description = "学历")
        private String degree;
        
        @Schema(description = "入学时间")
        private String startDate;
        
        @Schema(description = "毕业时间")
        private String endDate;
    }

    @Schema(description = "工作经历")
    @Data
    public static class WorkExperience {
        @Schema(description = "公司名称")
        private String company;
        
        @Schema(description = "职位名称")
        private String position;
        
        @Schema(description = "工作描述")
        private String description;
        
        @Schema(description = "开始时间")
        private String startDate;
        
        @Schema(description = "结束时间")
        private String endDate;
    }
}