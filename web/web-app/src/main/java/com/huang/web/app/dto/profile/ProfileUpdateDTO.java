package com.huang.web.app.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * 个人信息修改请求DTO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "个人信息修改请求")
@Data
public class ProfileUpdateDTO {

    @Schema(description = "昵称", example = "新昵称")
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Schema(description = "邮箱", example = "user@example.com")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Schema(description = "性别：0-未知，1-男，2-女", example = "1")
    @Min(value = 0, message = "性别值必须为0、1或2")
    @Max(value = 2, message = "性别值必须为0、1或2")
    private Integer gender;

    @Schema(description = "出生日期", example = "1990-01-01")
    private LocalDate birthDate;

    @Schema(description = "个人简介", example = "这是我的个人简介")
    @Size(max = 500, message = "个人简介长度不能超过500个字符")
    private String bio;

    @Schema(description = "地址", example = "北京市朝阳区")
    @Size(max = 200, message = "地址长度不能超过200个字符")
    private String address;

    @Schema(description = "职业", example = "软件工程师")
    @Size(max = 50, message = "职业长度不能超过50个字符")
    private String occupation;

    @Schema(description = "身高（厘米）", example = "175.5")
    @DecimalMin(value = "100.0", message = "身高必须大于100厘米")
    @DecimalMax(value = "250.0", message = "身高必须小于250厘米")
    private Double height;

    @Schema(description = "体重（公斤）", example = "70.2")
    @DecimalMin(value = "30.0", message = "体重必须大于30公斤")
    @DecimalMax(value = "200.0", message = "体重必须小于200公斤")
    private Double weight;
}