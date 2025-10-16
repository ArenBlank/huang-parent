package com.huang.web.admin.vo.store;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 门店统计VO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "门店统计VO")
@Data
public class GymStoreStatisticsVO {

    @Schema(description = "门店总数")
    private Integer totalStores;

    @Schema(description = "营业中门店数")
    private Integer activeStores;

    @Schema(description = "停业门店数")
    private Integer closedStores;

    @Schema(description = "装修中门店数")
    private Integer renovatingStores;

    @Schema(description = "总教练数")
    private Integer totalCoaches;

    @Schema(description = "总会员数")
    private Integer totalMembers;

    @Schema(description = "总课程数")
    private Integer totalCourses;
}
