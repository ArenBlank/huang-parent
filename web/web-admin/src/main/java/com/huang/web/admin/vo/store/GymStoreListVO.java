package com.huang.web.admin.vo.store;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 门店列表VO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "门店列表VO")
@Data
public class GymStoreListVO {

    @Schema(description = "门店ID")
    private Long id;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "门店编码")
    private String storeCode;

    @Schema(description = "门店地址")
    private String address;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "营业时间")
    private String businessHours;

    @Schema(description = "店长姓名")
    private String managerName;

    @Schema(description = "店长电话")
    private String managerPhone;

    @Schema(description = "营业面积(平米)")
    private Integer areaSize;

    @Schema(description = "最大容纳人数")
    private Integer maxCapacity;

    @Schema(description = "门店状态: 0-停业, 1-营业, 2-装修中")
    private Integer status;

    @Schema(description = "门店状态名称")
    private String statusName;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "教练数量")
    private Integer coachCount;

    @Schema(description = "会员数量")
    private Integer memberCount;
}
