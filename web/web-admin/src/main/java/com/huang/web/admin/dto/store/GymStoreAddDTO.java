package com.huang.web.admin.dto.store;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 门店新增DTO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "门店新增DTO")
@Data
public class GymStoreAddDTO {

    @Schema(description = "门店名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "朝阳健身中心")
    @NotBlank(message = "门店名称不能为空")
    private String storeName;

    @Schema(description = "门店编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "GYM001")
    @NotBlank(message = "门店编码不能为空")
    private String storeCode;

    @Schema(description = "门店地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京市朝阳区建国路88号")
    @NotBlank(message = "门店地址不能为空")
    private String address;

    @Schema(description = "联系电话", example = "010-12345678")
    @Pattern(regexp = "^1[3-9]\\d{9}$|^0\\d{2,3}-?\\d{7,8}$", message = "联系电话格式不正确")
    private String phone;

    @Schema(description = "营业时间", example = "06:00-22:00")
    private String businessHours;

    @Schema(description = "店长姓名", example = "张三")
    private String managerName;

    @Schema(description = "店长电话", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "店长电话格式不正确")
    private String managerPhone;

    @Schema(description = "营业面积(平米)", example = "1500")
    private Integer areaSize;

    @Schema(description = "设备数量", example = "50")
    private Integer equipmentCount;

    @Schema(description = "最大容纳人数", example = "200")
    private Integer maxCapacity;

    @Schema(description = "停车位数量", example = "30")
    private Integer parkingSpaces;

    @Schema(description = "设施信息JSON", example = "{\"wifi\":true,\"locker\":true,\"shower\":true}")
    private String facilities;

    @Schema(description = "门店描述", example = "位于市中心的高端健身会所")
    private String description;

    @Schema(description = "门店图片JSON(多张图片的URL数组)", example = "[\"https://xxx.com/img1.jpg\",\"https://xxx.com/img2.jpg\"]")
    private String images;

    @Schema(description = "纬度", example = "39.9042")
    private BigDecimal latitude;

    @Schema(description = "经度", example = "116.4074")
    private BigDecimal longitude;

    @Schema(description = "门店状态: 0-停业, 1-营业, 2-装修中", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "门店状态不能为空")
    private Integer status;
}
