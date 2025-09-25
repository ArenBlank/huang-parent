package com.huang.web.admin.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户详情视图对象
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "用户详情视图")
@Data
public class UserDetailVO {

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

    @Schema(description = "性别描述")
    private String genderDesc;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "状态：0-禁用，1-正常")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "角色列表")
    private List<UserRoleDetail> roles;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "登录次数")
    private Integer loginCount;

    @Schema(description = "用户角色详情")
    @Data
    public static class UserRoleDetail {
        
        @Schema(description = "角色ID")
        private Long roleId;
        
        @Schema(description = "角色名称")
        private String roleName;
        
        @Schema(description = "角色编码")
        private String roleCode;
        
        @Schema(description = "角色描述")
        private String roleDescription;
        
        @Schema(description = "分配时间")
        private LocalDateTime assignTime;
    }
}