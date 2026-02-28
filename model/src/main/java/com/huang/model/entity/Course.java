package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "课程")
@TableName(value = "course")
@Data
public class Course extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分类ID")
    @TableField(value = "category_id")
    private Long categoryId;

    @Schema(description = "标题")
    @TableField(value = "title")
    private String title;

    @Schema(description = "摘要")
    @TableField(value = "summary")
    private String summary;

    @Schema(description = "封面图")
    @TableField(value = "cover_url")
    private String coverUrl;

    @Schema(description = "等级")
    @TableField(value = "level")
    private String level;

    @Schema(description = "时长(分钟)")
    @TableField(value = "duration_min")
    private Integer durationMin;

    @Schema(description = "价格")
    @TableField(value = "price")
    private BigDecimal price;

    @Schema(description = "状态:0下架 1上架")
    @TableField(value = "status")
    private Integer status;
}
