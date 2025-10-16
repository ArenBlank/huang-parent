package com.huang.web.app.vo.store;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 门店详情VO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "门店详情VO")
@Data
public class GymStoreDetailVO {

    @Schema(description = "门店ID")
    private Long id;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "门店地址")
    private String address;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "营业时间")
    private String businessHours;

    @Schema(description = "营业面积(平米)")
    private Integer areaSize;

    @Schema(description = "设备数量")
    private Integer equipmentCount;

    @Schema(description = "最大容纳人数")
    private Integer maxCapacity;

    @Schema(description = "停车位数量")
    private Integer parkingSpaces;

    @Schema(description = "设施信息JSON字符串(数据库原始值,用于解析)")
    private String facilitiesJson;

    @Schema(description = "设施信息(解析后的对象)")
    private Object facilities;

    @Schema(description = "门店描述")
    private String description;

    @Schema(description = "门店图片JSON字符串(数据库原始值,用于解析)")
    private String images;

    @Schema(description = "门店图片列表")
    private List<String> imageList;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "距离(公里,如果用户提供了位置)")
    private Double distance;

    @Schema(description = "门店状态: 0-停业, 1-营业, 2-装修中")
    private Integer status;

    @Schema(description = "门店状态名称")
    private String statusName;

    @Schema(description = "教练数量")
    private Integer coachCount;

    @Schema(description = "今日可预约课程数")
    private Integer todayAvailableCourses;
}
