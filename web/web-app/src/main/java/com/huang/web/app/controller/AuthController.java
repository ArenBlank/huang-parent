package com.huang.web.app.controller;

import com.huang.common.result.Result;
import com.huang.common.utils.SmsCodeUtil;
import com.huang.common.utils.JwtUtil;
import com.huang.model.entity.Role;
import com.huang.model.entity.User;
import com.huang.model.entity.UserRole;
import com.huang.web.app.dto.auth.*;
import com.huang.web.app.service.core.RoleCoreService;
import com.huang.web.app.service.core.UserCoreService;
import com.huang.web.app.service.core.UserRoleCoreService;
import com.huang.web.app.vo.auth.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;

/**
 * App端认证控制器
 * @author system
 * @since 2026-02-25
 */
@Tag(name = "App端认证管理", description = "用户注册、登录、验证码等功能")
@Slf4j
@RestController
@RequestMapping("/app/auth")
@Validated
public class AuthController {

    @Autowired
    private SmsCodeUtil smsCodeUtil;

    @Autowired
    private UserCoreService userCoreService;

    @Autowired
    private RoleCoreService roleCoreService;

    @Autowired
    private UserRoleCoreService userRoleCoreService;

    @Operation(summary = "发送短信验证码", description = "发送注册、登录或重置密码验证码")
    @PostMapping("/sms-code/send")
    public Result<String> sendSmsCode(@Valid @RequestBody SmsCodeDTO dto) {
        log.info("发送短信验证码请求: 手机号={}, 类型={}", dto.getPhone(), dto.getType());
        
        // 检查发送频率限制
        if (!smsCodeUtil.canSendSms(dto.getPhone(), dto.getType())) {
            return Result.fail("发送过于频繁，请稍后再试");
        }
        
        // 发送验证码
        String code = smsCodeUtil.sendSmsCode(dto.getPhone(), dto.getType());
        
        if (code != null) {
            // 开发模式下返回验证码
            return Result.ok("验证码发送成功，开发模式验证码: " + code);
        }
        
        return Result.ok("验证码发送成功");
    }

    @Operation(summary = "用户注册", description = "新用户注册")
    @PostMapping("/register")
    public Result<RegisterVO> register(@Valid @RequestBody UserRegisterDTO dto) {
        log.info("用户注册请求: 用户名={}, 手机号={}", dto.getUsername(), dto.getPhone());
        
        // 验证确认密码
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return Result.fail("两次输入的密码不一致");
        }
        
        // 验证短信验证码
        if (!smsCodeUtil.verifySmsCode(dto.getPhone(), dto.getSmsCode(), "register")) {
            return Result.fail("验证码错误或已失效");
        }
        
        if (userCoreService.existsByUsername(dto.getUsername())) {
            return Result.fail("用户名已存在");
        }
        if (userCoreService.existsByPhone(dto.getPhone())) {
            return Result.fail("手机号已注册");
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank() && userCoreService.existsByEmail(dto.getEmail())) {
            return Result.fail("邮箱已注册");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setGender(dto.getGender());
        user.setBirthDate(dto.getBirthDate());
        user.setStatus(1);
        user.setUserType("member");
        userCoreService.save(user);

        Role memberRole = roleCoreService.getByRoleCode("MEMBER");
        if (memberRole != null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(memberRole.getId());
            userRoleCoreService.save(userRole);
        }

        RegisterVO vo = new RegisterVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setRegisterTime(LocalDateTime.now());
        vo.setAutoLogin(true);
        vo.setAccessToken(JwtUtil.generateAppAccessToken(user.getId(), user.getUsername()));
        vo.setRefreshToken(JwtUtil.generateAppRefreshToken(user.getId(), user.getUsername()));
        vo.setWelcomeMessage("注册成功，欢迎加入健身平台！");
        
        log.info("用户注册成功: 用户ID={}, 用户名={}", vo.getUserId(), vo.getUsername());
        return Result.ok(vo);
    }

    @Operation(summary = "用户登录", description = "密码登录或短信验证码登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody UserLoginDTO dto) {
        log.info("用户登录请求: 账号={}, 登录类型={}", dto.getAccount(), dto.getLoginType());
        
        if ("sms".equals(dto.getLoginType())) {
            // 短信登录验证
            if (!smsCodeUtil.verifySmsCode(dto.getAccount(), dto.getSmsCode(), "login")) {
                return Result.fail("验证码错误或已失效");
            }
        }
        
        User user = findByAccount(dto.getAccount());
        if (user == null) {
            return Result.fail("账号不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            return Result.fail("账号已禁用");
        }

        if ("password".equals(dto.getLoginType())) {
            if (!dto.getPassword().equals(user.getPassword())) {
                return Result.fail("账号或密码错误");
            }
        }

        LoginVO vo = new LoginVO();
        UserInfoVO userInfo = new UserInfoVO();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setPhone(user.getPhone());
        userInfo.setEmail(user.getEmail());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setGender(user.getGender());
        userInfo.setStatus(user.getStatus());
        
        vo.setUserInfo(userInfo);
        vo.setFirstLogin(false);
        vo.setNeedCompleteProfile(false);

        vo.setAccessToken(JwtUtil.generateAppAccessToken(user.getId(), user.getUsername()));
        vo.setRefreshToken(JwtUtil.generateAppRefreshToken(user.getId(), user.getUsername()));
        vo.setAccessTokenExpire(LocalDateTime.now().plusDays(7));
        vo.setRefreshTokenExpire(LocalDateTime.now().plusDays(30));
        
        log.info("用户登录成功: 用户ID={}, 用户名={}", userInfo.getId(), userInfo.getUsername());
        return Result.ok(vo);
    }

    @Operation(summary = "刷新令牌", description = "使用refresh token获取新的access token")
    @PostMapping("/refresh-token")
    public Result<RefreshTokenVO> refreshToken(@Valid @RequestBody RefreshTokenDTO dto) {
        log.info("刷新令牌请求: refreshToken前8位={}", 
                dto.getRefreshToken().length() > 8 ? dto.getRefreshToken().substring(0, 8) + "..." : dto.getRefreshToken());
        
        String newAccessToken = JwtUtil.refreshAccessToken(dto.getRefreshToken());
        if (newAccessToken == null) {
            return Result.fail("刷新令牌无效或已过期");
        }
        RefreshTokenVO vo = new RefreshTokenVO();
        vo.setAccessToken(newAccessToken);
        vo.setRefreshToken(dto.getRefreshToken());
        vo.setAccessTokenExpire(LocalDateTime.now().plusDays(7));
        vo.setRefreshTokenExpire(LocalDateTime.now().plusDays(30));
        
        log.info("令牌刷新成功");
        return Result.ok(vo);
    }

    @Operation(summary = "忘记密码", description = "通过短信验证码重置密码")
    @PostMapping("/forget-password")
    public Result<String> forgetPassword(@Valid @RequestBody ForgetPasswordDTO dto) {
        log.info("忘记密码请求: 手机号={}", dto.getPhone());
        
        // 验证确认密码
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return Result.fail("两次输入的密码不一致");
        }
        
        // 验证短信验证码
        if (!smsCodeUtil.verifySmsCode(dto.getPhone(), dto.getSmsCode(), "reset_password")) {
            return Result.fail("验证码错误或已失效");
        }
        
        User user = userCoreService.getByPhone(dto.getPhone());
        if (user == null) {
            return Result.fail("手机号未注册");
        }
        user.setPassword(dto.getNewPassword());
        userCoreService.updateById(user);
        
        log.info("密码重置成功: 手机号={}", dto.getPhone());
        return Result.ok("密码重置成功");
    }

    @Operation(summary = "退出登录", description = "用户退出登录")
    @PostMapping("/logout")
    public Result<String> logout() {
        log.info("用户退出登录成功");
        return Result.ok("退出登录成功");
    }

    private User findByAccount(String account) {
        if (account == null || account.isBlank()) {
            return null;
        }
        User byPhone = userCoreService.getByPhone(account);
        if (byPhone != null) {
            return byPhone;
        }
        return userCoreService.getByUsername(account);
    }
}
