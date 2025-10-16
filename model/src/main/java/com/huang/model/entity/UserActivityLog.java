package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户活动日志表
 */
@Data
@TableName("user_activity_log")
public class UserActivityLog {

    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 活动日期
     */
    private LocalDate activityDate;

    /**
     * 活动类型: login登录 course_enroll课程报名 exercise运动记录等
     */
    private String activityType;

    /**
     * 活动次数
     */
    private Integer activityCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
