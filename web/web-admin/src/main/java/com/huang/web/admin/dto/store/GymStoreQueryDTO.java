package com.huang.web.admin.dto.store;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

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

    @Schema(description = "门店编码", example = "GYM001")
    private String storeCode;

    @Schema(description = "门店地址(模糊查询)", example = "北京市朝阳区")
    private String address;

    @Schema(description = "门店状态: 0-停业, 1-营业, 2-装修中", example = "1")
    private Integer status;

    @Schema(description = "店长姓名(模糊查询)", example = "张三")
    private String managerName;

    @Schema(description = "联系电话(模糊查询)", example = "138")
    private String phone;
}
