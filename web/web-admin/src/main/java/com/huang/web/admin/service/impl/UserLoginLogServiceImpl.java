package com.huang.web.admin.service.impl;

import com.huang.model.entity.UserLoginLog;
import com.huang.web.admin.mapper.UserLoginLogMapper;
import com.huang.web.admin.service.UserLoginLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户登录日志服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginLogServiceImpl implements UserLoginLogService {

    private final UserLoginLogMapper userLoginLogMapper;

    @Override
    public void recordLoginLog(UserLoginLog loginLog) {
        try {
            userLoginLogMapper.insert(loginLog);
        } catch (Exception e) {
            log.error("记录登录日志失败", e);
        }
    }

    @Override
    public void recordLoginSuccess(Long userId, String username, String ip, String device) {
        UserLoginLog loginLog = new UserLoginLog();
        loginLog.setUserId(userId);
        loginLog.setUsername(username);
        loginLog.setLoginTime(LocalDateTime.now());
        loginLog.setLoginIp(ip);
        loginLog.setLoginDevice(device);
        loginLog.setLoginType("normal");
        loginLog.setLoginStatus(1);
        loginLog.setCreateTime(LocalDateTime.now());

        recordLoginLog(loginLog);
    }

    @Override
    public void recordLoginFailure(String username, String ip, String device, String failReason) {
        UserLoginLog loginLog = new UserLoginLog();
        loginLog.setUsername(username);
        loginLog.setLoginTime(LocalDateTime.now());
        loginLog.setLoginIp(ip);
        loginLog.setLoginDevice(device);
        loginLog.setLoginType("normal");
        loginLog.setLoginStatus(0);
        loginLog.setFailReason(failReason);
        loginLog.setCreateTime(LocalDateTime.now());

        recordLoginLog(loginLog);
    }
}
