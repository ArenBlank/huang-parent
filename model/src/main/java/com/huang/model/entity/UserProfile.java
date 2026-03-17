package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "User profile")
@TableName("user_profile")
@Data
public class UserProfile extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "User ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "Bio")
    @TableField("bio")
    private String bio;

    @Schema(description = "Address")
    @TableField("address")
    private String address;

    @Schema(description = "Occupation")
    @TableField("occupation")
    private String occupation;

    @Schema(description = "Height (cm)")
    @TableField("height")
    private Integer height;

    @Schema(description = "Weight (kg)")
    @TableField("weight")
    private Integer weight;
}
