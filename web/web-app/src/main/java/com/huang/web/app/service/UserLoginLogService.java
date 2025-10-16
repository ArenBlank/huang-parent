package com.huang.web.app.service;

import com.huang.model.entity.UserLoginLog;

/**
 * App端用户登录日志服务接口
 */
public interface UserLoginLogService {

    /**
     * 记录用户登录日志
     */
    void recordLoginLog(UserLoginLog loginLog);

    /**
     * 记录用户登录成功
     */
    void recordLoginSuccess(Long userId, String username, String ip, String device);

    /**
     * 记录用户登录失败
     */
    void recordLoginFailure(String username, String ip, String device, String failReason);
}
