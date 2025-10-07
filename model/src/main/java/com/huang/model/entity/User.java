package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "用户表")
@TableName(value = "user")
@Data
public class User extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名")
    @TableField(value = "username")
    private String username;

    @Schema(description = "密码")
    @TableField(value = "password")
    private String password;

    @Schema(description = "昵称")
    @TableField(value = "nickname")
    private String nickname;

    @Schema(description = "邮箱")
    @TableField(value = "email")
    private String email;

    @Schema(description = "手机号")
    @TableField(value = "phone")
    private String phone;

    @Schema(description = "头像")
    @TableField(value = "avatar")
    private String avatar;

    @Schema(description = "性别：0-未知，1-男，2-女")
    @TableField(value = "gender")
    private Integer gender;

    @Schema(description = "出生日期")
    @TableField(value = "birth_date")
    private LocalDate birthDate;

    @Schema(description = "状态：0-禁用，1-正常")
    @TableField(value = "status")
    private Integer status;

    @Schema(description = "最后登录时间")
    @TableField(value = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Schema(description = "积分")
    @TableField(value = "points")
    private Integer points;

    @Schema(description = "VIP等级")
    @TableField(value = "vip_level")
    private Integer vipLevel;

    @Schema(description = "个人简介")
    @TableField(value = "bio")
    private String bio;

    @Schema(description = "地址")
    @TableField(value = "address")
    private String address;

    @Schema(description = "职业")
    @TableField(value = "occupation")
    private String occupation;

    @Schema(description = "身高(cm)")
    @TableField(value = "height")
    private Double height;

    @Schema(description = "体重(kg)")
    @TableField(value = "weight")
    private Double weight;
}
