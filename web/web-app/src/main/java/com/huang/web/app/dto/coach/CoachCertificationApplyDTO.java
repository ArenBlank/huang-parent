package com.huang.web.app.dto.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.util.List;

/**
 * 教练认证申请DTO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "教练认证申请")
@Data
public class CoachCertificationApplyDTO {

    @Schema(description = "真实姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张教练")
    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    private String realName;

    @Schema(description = "身份证号", requiredMode = Schema.RequiredMode.REQUIRED, example = "110101199001011234")
    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$", 
             message = "身份证号格式不正确")
    private String idCard;

    @Schema(description = "联系电话", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138001")
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "邮箱", example = "coach@example.com")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Schema(description = "专长领域", requiredMode = Schema.RequiredMode.REQUIRED, example = "瑜伽,力量训练,有氧运动")
    @NotBlank(message = "专长领域不能为空")
    @Size(max = 200, message = "专长领域长度不能超过200个字符")
    private String specialties;

    @Schema(description = "从业年限", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    @NotNull(message = "从业年限不能为空")
    @Min(value = 0, message = "从业年限不能小于0年")
    @Max(value = 50, message = "从业年限不能超过50年")
    private Integer experienceYears;

    @Schema(description = "个人介绍", requiredMode = Schema.RequiredMode.REQUIRED, example = "拥有5年健身指导经验，擅长瑜伽和力量训练")
    @NotBlank(message = "个人介绍不能为空")
    @Size(min = 10, max = 1000, message = "个人介绍长度需要在10-1000个字符之间")
    private String introduction;

    @Schema(description = "证书信息列表")
    private List<CertificateInfo> certificates;

    @Schema(description = "教育背景列表")
    private List<EducationInfo> education;

    @Schema(description = "工作经历列表")
    private List<WorkExperience> workExperience;

    @Schema(description = "证书信息")
    @Data
    public static class CertificateInfo {
        @Schema(description = "证书名称", example = "国际瑜伽教练资格证")
        private String name;
        
        @Schema(description = "颁发机构", example = "国际瑜伽联盟")
        private String issuer;
        
        @Schema(description = "获得时间", example = "2020-06-15")
        private String issueDate;
        
        @Schema(description = "证书编号", example = "YGA20200615001")
        private String certificateNo;
        
        @Schema(description = "证书图片URL", example = "/uploads/certificates/cert_001.jpg")
        private String imageUrl;
    }

    @Schema(description = "教育背景")
    @Data
    public static class EducationInfo {
        @Schema(description = "学校名称", example = "北京体育大学")
        private String school;
        
        @Schema(description = "专业名称", example = "运动训练学")
        private String major;
        
        @Schema(description = "学历", example = "本科")
        private String degree;
        
        @Schema(description = "入学时间", example = "2015-09")
        private String startDate;
        
        @Schema(description = "毕业时间", example = "2019-06")
        private String endDate;
    }

    @Schema(description = "工作经历")
    @Data
    public static class WorkExperience {
        @Schema(description = "公司名称", example = "某健身俱乐部")
        private String company;
        
        @Schema(description = "职位名称", example = "高级健身教练")
        private String position;
        
        @Schema(description = "工作描述", example = "负责会员健身指导和课程安排")
        private String description;
        
        @Schema(description = "开始时间", example = "2019-07")
        private String startDate;
        
        @Schema(description = "结束时间", example = "2023-12")
        private String endDate;
    }
}