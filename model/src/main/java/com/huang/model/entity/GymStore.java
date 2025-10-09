package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 门店管理表
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "门店管理表")
@TableName(value = "gym_store")
@Data
public class GymStore extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "门店名称")
    @TableField(value = "store_name")
    private String storeName;

    @Schema(description = "门店编码")
    @TableField(value = "store_code")
    private String storeCode;

    @Schema(description = "门店地址")
    @TableField(value = "address")
    private String address;

    @Schema(description = "联系电话")
    @TableField(value = "phone")
    private String phone;

    @Schema(description = "营业时间")
    @TableField(value = "business_hours")
    private String businessHours;

    @Schema(description = "店长姓名")
    @TableField(value = "manager_name")
    private String managerName;

    @Schema(description = "店长电话")
    @TableField(value = "manager_phone")
    private String managerPhone;

    @Schema(description = "营业面积(平米)")
    @TableField(value = "area_size")
    private Integer areaSize;

    @Schema(description = "设备数量")
    @TableField(value = "equipment_count")
    private Integer equipmentCount;

    @Schema(description = "最大容纳人数")
    @TableField(value = "max_capacity")
    private Integer maxCapacity;

    @Schema(description = "停车位数量")
    @TableField(value = "parking_spaces")
    private Integer parkingSpaces;

    @Schema(description = "设施信息JSON")
    @TableField(value = "facilities")
    private String facilities;

    @Schema(description = "门店描述")
    @TableField(value = "description")
    private String description;

    @Schema(description = "门店图片JSON")
    @TableField(value = "images")
    private String images;

    @Schema(description = "纬度")
    @TableField(value = "latitude")
    private BigDecimal latitude;

    @Schema(description = "经度")
    @TableField(value = "longitude")
    private BigDecimal longitude;

    @Schema(description = "状态：0-停业，1-营业，2-装修中")
    @TableField(value = "status")
    private Integer status;
}