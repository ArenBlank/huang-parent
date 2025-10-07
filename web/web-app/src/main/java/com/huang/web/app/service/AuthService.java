package com.huang.web.app.service;

import com.huang.web.app.dto.auth.*;
import com.huang.web.app.vo.auth.*;

/**
 * App端认证业务服务接口
 * @author system
 * @since 2025-01-24
 */
public interface AuthService {

    /**
     * 发送短信验证码
     * @param dto 短信验证码请求参数
     * @return 发送结果消息
     */
    String sendSmsCode(SmsCodeDTO dto);

    /**
     * 用户注册
     * @param dto 用户注册请求参数
     * @return 注册结果
     */
    RegisterVO register(UserRegisterDTO dto);

    /**
     * 用户登录
     * @param dto 用户登录请求参数
     * @return 登录结果
     */
    LoginVO login(UserLoginDTO dto);

    /**
     * 刷新访问令牌
     * @param dto 刷新令牌请求参数
     * @return 新的令牌信息
     */
    RefreshTokenVO refreshToken(RefreshTokenDTO dto);

    /**
     * 忘记密码重置
     * @param dto 忘记密码请求参数
     * @return 重置结果消息
     */
    String forgetPassword(ForgetPasswordDTO dto);

    /**
     * 用户退出登录
     * @param token 访问令牌
     * @return 退出结果消息
     */
    String logout(String token);
}