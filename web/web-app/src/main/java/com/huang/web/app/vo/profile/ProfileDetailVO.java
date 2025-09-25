package com.huang.web.app.vo.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 个人信息详情VO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "个人信息详情")
@Data
public class ProfileDetailVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "性别：0-未知，1-男，2-女")
    private Integer gender;

    @Schema(description = "性别显示文本")
    private String genderText;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "年龄")
    private Integer age;

    @Schema(description = "个人简介")
    private String bio;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "职业")
    private String occupation;

    @Schema(description = "身高（厘米）")
    private Integer height;

    @Schema(description = "体重（公斤）")
    private Integer weight;

    @Schema(description = "状态：0-禁用，1-正常")
    private Integer status;

    @Schema(description = "VIP等级")
    private Integer vipLevel;

    @Schema(description = "VIP到期时间")
    private LocalDateTime vipExpireTime;

    @Schema(description = "注册时间")
    private LocalDateTime createTime;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "积分余额")
    private Integer points;

    @Schema(description = "是否完善资料")
    private Boolean profileCompleted;

    @Schema(description = "资料完成度（百分比）")
    private Integer completionRate;
}