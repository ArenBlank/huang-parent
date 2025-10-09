package com.huang.web.admin.dto.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 教练认证申请查询DTO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "教练认证申请查询DTO")
@Data
public class CoachCertificationQueryDTO {

    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码不能小于1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "10")
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer size = 10;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "状态: pending待审核 approved已通过 rejected已拒绝")
    private String status;

    @Schema(description = "专长领域")
    private String specialties;

    @Schema(description = "申请开始时间", example = "2025-01-01 00:00:00")
    private String startTime;

    @Schema(description = "申请结束时间", example = "2025-12-31 23:59:59")
    private String endTime;

    @Schema(description = "排序方式: asc升序 desc降序", example = "desc")
    private String sortOrder = "desc";
}