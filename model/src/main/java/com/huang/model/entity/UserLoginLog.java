package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户登录日志表
 */
@Data
@TableName("user_login_log")
public class UserLoginLog {

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
     * 用户名
     */
    private String username;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 登录IP
     */
    private String loginIp;

    /**
     * 登录设备
     */
    private String loginDevice;

    /**
     * 操作系统
     */
    private String loginOs;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 登录类型: normal正常 oauth第三方
     */
    private String loginType;

    /**
     * 登录状态: 0失败 1成功
     */
    private Integer loginStatus;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
