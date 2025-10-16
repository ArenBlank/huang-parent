package com.huang.web.app.dto.store;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 门店查询DTO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "门店查询DTO")
@Data
public class GymStoreQueryDTO {

    @Schema(description = "当前页码", example = "1")
    private Long current = 1L;

    @Schema(description = "每页大小", example = "10")
    private Long size = 10L;

    @Schema(description = "门店名称(模糊查询)", example = "朝阳店")
    private String storeName;

    @Schema(description = "门店地址(模糊查询)", example = "北京市朝阳区")
    private String address;

    @Schema(description = "用户当前纬度(用于附近门店查询)", example = "39.9042")
    private BigDecimal userLatitude;

    @Schema(description = "用户当前经度(用于附近门店查询)", example = "116.4074")
    private BigDecimal userLongitude;

    @Schema(description = "搜索半径(公里,用于附近门店查询)", example = "5")
    private Integer radius;
}
