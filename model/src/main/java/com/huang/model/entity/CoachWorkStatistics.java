package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 教练工作统计表
 */
@Data
@TableName("coach_work_statistics")
public class CoachWorkStatistics {

    /**
     * 统计ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 教练ID
     */
    private Long coachId;

    /**
     * 教练姓名
     */
    private String coachName;

    /**
     * 统计日期
     */
    private LocalDate statisticsDate;

    /**
     * 授课次数
     */
    private Integer courseCount;

    /**
     * 授课时长(小时)
     */
    private BigDecimal courseHours;

    /**
     * 服务学员数
     */
    private Integer studentCount;

    /**
     * 咨询次数
     */
    private Integer consultationCount;

    /**
     * 收入金额
     */
    private BigDecimal incomeAmount;

    /**
     * 平均评分
     */
    private BigDecimal ratingAvg;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
