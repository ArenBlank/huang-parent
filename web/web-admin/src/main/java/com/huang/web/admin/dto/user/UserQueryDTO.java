package com.huang.web.admin.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户查询请求DTO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "用户查询请求")
@Data
public class UserQueryDTO {

    @Schema(description = "页码", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", defaultValue = "10")
    private Integer size = 10;

    @Schema(description = "用户名（模糊查询）")
    private String username;

    @Schema(description = "昵称（模糊查询）")
    private String nickname;

    @Schema(description = "邮箱（精确查询）")
    private String email;

    @Schema(description = "手机号（精确查询）")
    private String phone;

    @Schema(description = "性别：0-未知，1-男，2-女")
    private Integer gender;

    @Schema(description = "状态：0-禁用，1-正常")
    private Integer status;

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "开始时间（创建时间筛选）")
    private String startTime;

    @Schema(description = "结束时间（创建时间筛选）")
    private String endTime;

    @Schema(description = "排序字段", defaultValue = "createTime")
    private String sortField = "createTime";

    @Schema(description = "排序方式：asc-升序，desc-降序", defaultValue = "desc")
    private String sortOrder = "desc";
}