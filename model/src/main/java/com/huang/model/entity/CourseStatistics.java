package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 课程统计表
 */
@Data
@TableName("course_statistics")
public class CourseStatistics {

    /**
     * 统计ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 统计日期
     */
    private LocalDate statisticsDate;

    /**
     * 新增报名数
     */
    private Integer newEnrollmentCount;

    /**
     * 取消报名数
     */
    private Integer cancelEnrollmentCount;

    /**
     * 完成课程数
     */
    private Integer completedCourseCount;

    /**
     * 签到人数
     */
    private Integer checkInCount;

    /**
     * 总参与人数
     */
    private Integer totalParticipants;

    /**
     * 最受欢迎课程ID
     */
    private Long popularCourseId;

    /**
     * 最受欢迎课程名称
     */
    private String popularCourseName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
