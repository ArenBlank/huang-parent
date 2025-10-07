package com.huang.web.app.dto.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * App端角色查询请求DTO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "角色查询请求")
@Data
public class RoleQueryDTO {

    @Schema(description = "页码", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", defaultValue = "10")
    private Integer size = 10;

    @Schema(description = "角色名称（模糊查询）")
    private String roleName;

    @Schema(description = "角色编码（模糊查询）")
    private String roleCode;

    @Schema(description = "状态：0-禁用，1-正常")
    private Integer status;

    @Schema(description = "开始时间（创建时间筛选）")
    private String startTime;

    @Schema(description = "结束时间（创建时间筛选）")
    private String endTime;

    @Schema(description = "排序方式：asc-升序，desc-降序", defaultValue = "desc")
    private String sortOrder = "desc";
}