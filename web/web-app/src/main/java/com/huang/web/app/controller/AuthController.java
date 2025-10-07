package com.huang.web.app.controller;

import com.huang.common.result.Result;
import com.huang.web.app.dto.auth.*;
import com.huang.web.app.service.AuthService;
import com.huang.web.app.vo.auth.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * App端认证控制器
 * @author system
 * @since 2025-01-24
 */
@Tag(name = "App端认证管理", description = "用户注册、登录、验证码等功能")
@Slf4j
@RestController
@RequestMapping("/app/auth")
@Validated
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "发送短信验证码", description = "发送注册、登录或重置密码验证码")
    @PostMapping("/sms-code/send")
    public Result<String> sendSmsCode(@Valid @RequestBody SmsCodeDTO dto) {
        try {
            String message = authService.sendSmsCode(dto);
            return Result.ok(message);
        } catch (RuntimeException e) {
            log.error("发送短信验证码失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("发送短信验证码失败", e);
            return Result.fail("发送短信验证码失败：" + e.getMessage());
        }
    }

    @Operation(summary = "用户注册", description = "新用户注册")
    @PostMapping("/register")
    public Result<RegisterVO> register(@Valid @RequestBody UserRegisterDTO dto) {
        try {
            RegisterVO vo = authService.register(dto);
            return Result.ok(vo);
        } catch (RuntimeException e) {
            log.error("用户注册失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("用户注册失败", e);
            return Result.fail("用户注册失败：" + e.getMessage());
        }
    }

    @Operation(summary = "用户登录", description = "密码登录或短信验证码登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody UserLoginDTO dto) {
        try {
            LoginVO vo = authService.login(dto);
            return Result.ok(vo);
        } catch (RuntimeException e) {
            log.error("用户登录失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("用户登录失败", e);
            return Result.fail("用户登录失败：" + e.getMessage());
        }
    }

    @Operation(summary = "刷新令牌", description = "使用refresh token获取新的access token")
    @PostMapping("/refresh-token")
    public Result<RefreshTokenVO> refreshToken(@Valid @RequestBody RefreshTokenDTO dto) {
        try {
            RefreshTokenVO vo = authService.refreshToken(dto);
            return Result.ok(vo);
        } catch (RuntimeException e) {
            log.error("刷新令牌失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("刷新令牌失败", e);
            return Result.fail("刷新令牌失败：" + e.getMessage());
        }
    }

    @Operation(summary = "忘记密码", description = "通过短信验证码重置密码")
    @PostMapping("/forget-password")
    public Result<String> forgetPassword(@Valid @RequestBody ForgetPasswordDTO dto) {
        try {
            String message = authService.forgetPassword(dto);
            return Result.ok(message);
        } catch (RuntimeException e) {
            log.error("忘记密码处理失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("忘记密码处理失败", e);
            return Result.fail("忘记密码处理失败：" + e.getMessage());
        }
    }

    @Operation(summary = "退出登录", description = "用户退出登录")
    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request) {
        try {
            // 从请求头获取令牌
            String authHeader = request.getHeader("Authorization");
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
            
            String message = authService.logout(token);
            return Result.ok(message);
        } catch (Exception e) {
            log.error("退出登录失败", e);
            return Result.fail("退出登录失败：" + e.getMessage());
        }
    }
}