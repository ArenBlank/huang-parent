package com.huang.web.app.controller;

import com.huang.common.config.DevelopmentConfig;
import com.huang.common.constant.RedisConstant;
import com.huang.common.guard.RateLimit;
import com.huang.common.result.Result;
import com.huang.common.utils.JwtUtil;
import com.huang.common.utils.PasswordUtil;
import com.huang.common.utils.SmsCodeUtil;
import com.huang.model.entity.Role;
import com.huang.model.entity.User;
import com.huang.model.entity.UserRole;
import com.huang.web.app.dto.auth.ForgetPasswordDTO;
import com.huang.web.app.dto.auth.RefreshTokenDTO;
import com.huang.web.app.dto.auth.SmsCodeDTO;
import com.huang.web.app.dto.auth.UserLoginDTO;
import com.huang.web.app.dto.auth.UserRegisterDTO;
import com.huang.web.app.service.core.RoleCoreService;
import com.huang.web.app.service.core.UserCoreService;
import com.huang.web.app.service.core.UserRoleCoreService;
import com.huang.web.app.vo.auth.LoginVO;
import com.huang.web.app.vo.auth.RefreshTokenVO;
import com.huang.web.app.vo.auth.RegisterVO;
import com.huang.web.app.vo.auth.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "App认证", description = "用户注册、登录、验证码、重置密码")
@Slf4j
@RestController
@RequestMapping("/app/auth")
@Validated
public class AuthController {

    private final SmsCodeUtil smsCodeUtil;
    private final UserCoreService userCoreService;
    private final RoleCoreService roleCoreService;
    private final UserRoleCoreService userRoleCoreService;

    public AuthController(SmsCodeUtil smsCodeUtil,
                          UserCoreService userCoreService,
                          RoleCoreService roleCoreService,
                          UserRoleCoreService userRoleCoreService) {
        this.smsCodeUtil = smsCodeUtil;
        this.userCoreService = userCoreService;
        this.roleCoreService = roleCoreService;
        this.userRoleCoreService = userRoleCoreService;
    }

    @Operation(summary = "发送短信验证码", description = "发送注册、登录、重置密码验证码")
    @RateLimit(
            prefix = RedisConstant.APP_SMS_SEND_LIMIT_PREFIX,
            key = "#dto.type + ':' + #dto.phone",
            maxRequests = RedisConstant.SMS_SEND_RATE_LIMIT_MAX,
            windowSec = RedisConstant.SMS_SEND_RATE_LIMIT_WINDOW_SEC,
            message = "短信发送过于频繁，请稍后再试"
    )
    @PostMapping("/sms-code/send")
    public Result<String> sendSmsCode(@Valid @RequestBody SmsCodeDTO dto) {
        log.info("发送短信验证码请求: phone={}, type={}", dto.getPhone(), dto.getType());

        if (!smsCodeUtil.canSendSms(dto.getPhone(), dto.getType())) {
            return Result.fail("发送过于频繁，请稍后再试");
        }

        String code = smsCodeUtil.sendSmsCode(dto.getPhone(), dto.getType());
        if (code != null) {
            return Result.ok("验证码发送成功，开发模式验证码: " + code);
        }
        return Result.ok("验证码发送成功");
    }

    @Operation(summary = "用户注册", description = "新用户注册")
    @PostMapping("/register")
    public Result<RegisterVO> register(@Valid @RequestBody UserRegisterDTO dto) {
        log.info("用户注册请求: username={}, phone={}", dto.getUsername(), dto.getPhone());

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return Result.fail("两次输入的密码不一致");
        }
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
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
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
        vo.setAccessToken(JwtUtil.generateAppAccessToken(user.getId(), user.getUsername(), normalizeTokenVersion(user)));
        vo.setRefreshToken(JwtUtil.generateAppRefreshToken(user.getId(), user.getUsername(), normalizeTokenVersion(user)));
        vo.setWelcomeMessage("注册成功，欢迎加入健身平台");

        log.info("用户注册成功: userId={}, username={}", vo.getUserId(), vo.getUsername());
        return Result.ok(vo);
    }

    @Operation(summary = "用户登录", description = "密码登录或短信验证码登录")
    @RateLimit(
            prefix = RedisConstant.APP_LOGIN_LIMIT_IP_PREFIX,
            key = "#ip",
            maxRequests = RedisConstant.LOGIN_RATE_LIMIT_MAX,
            windowSec = RedisConstant.LOGIN_RATE_LIMIT_WINDOW_SEC,
            message = "登录请求过于频繁，请稍后再试"
    )
    @RateLimit(
            prefix = RedisConstant.APP_LOGIN_LIMIT_ACCOUNT_PREFIX,
            key = "#dto.account",
            maxRequests = RedisConstant.LOGIN_RATE_LIMIT_MAX,
            windowSec = RedisConstant.LOGIN_RATE_LIMIT_WINDOW_SEC,
            message = "登录请求过于频繁，请稍后再试"
    )
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody UserLoginDTO dto) {
        log.info("用户登录请求: account={}, loginType={}", dto.getAccount(), dto.getLoginType());

        if ("sms".equals(dto.getLoginType())
                && !smsCodeUtil.verifySmsCode(dto.getAccount(), dto.getSmsCode(), "login")) {
            return Result.fail("验证码错误或已失效");
        }

        User user = findByAccount(dto.getAccount());
        if (user == null) {
            return Result.fail("账号不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            return Result.fail("账号已禁用");
        }

        if ("password".equals(dto.getLoginType()) && !PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            return Result.fail("账号或密码错误");
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
        vo.setAccessToken(JwtUtil.generateAppAccessToken(user.getId(), user.getUsername(), normalizeTokenVersion(user)));
        vo.setRefreshToken(JwtUtil.generateAppRefreshToken(user.getId(), user.getUsername(), normalizeTokenVersion(user)));
        vo.setAccessTokenExpire(LocalDateTime.now().plusSeconds(JwtUtil.accessTokenExpireMs(JwtUtil.PLATFORM_APP) / 1000));
        vo.setRefreshTokenExpire(LocalDateTime.now().plusSeconds(JwtUtil.refreshTokenExpireMs(JwtUtil.PLATFORM_APP) / 1000));

        log.info("用户登录成功: userId={}, username={}", userInfo.getId(), userInfo.getUsername());
        return Result.ok(vo);
    }

    @Operation(summary = "刷新令牌", description = "使用 refresh token 获取新的 access token")
    @PostMapping("/refresh-token")
    public Result<RefreshTokenVO> refreshToken(@Valid @RequestBody RefreshTokenDTO dto) {
        log.info("刷新令牌请求: refreshToken前缀={}",
                dto.getRefreshToken().length() > 8 ? dto.getRefreshToken().substring(0, 8) + "..." : dto.getRefreshToken());

        var claims = JwtUtil.parseTokenSafely(dto.getRefreshToken());
        if (claims == null || !JwtUtil.isRefreshToken(claims)) {
            return Result.fail("刷新令牌无效或已过期");
        }
        Long userId = JwtUtil.getUserIdFromToken(dto.getRefreshToken());
        User user = userId == null ? null : userCoreService.getById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            return Result.fail("刷新令牌无效或已过期");
        }
        if (normalizeTokenVersion(user) != JwtUtil.getTokenVersionFromClaims(claims)) {
            return Result.fail("刷新令牌无效或已过期");
        }
        String newAccessToken = JwtUtil.generateAccessToken(
                user.getId(),
                user.getUsername(),
                JwtUtil.getPlatformFromClaims(claims),
                normalizeTokenVersion(user)
        );

        RefreshTokenVO vo = new RefreshTokenVO();
        vo.setAccessToken(newAccessToken);
        vo.setRefreshToken(dto.getRefreshToken());
        vo.setAccessTokenExpire(LocalDateTime.now().plusSeconds(JwtUtil.accessTokenExpireMs(JwtUtil.getPlatformFromClaims(claims)) / 1000));
        vo.setRefreshTokenExpire(LocalDateTime.now().plusSeconds(JwtUtil.refreshTokenExpireMs(JwtUtil.getPlatformFromClaims(claims)) / 1000));

        log.info("令牌刷新成功");
        return Result.ok(vo);
    }

    @Operation(summary = "忘记密码", description = "通过短信验证码重置密码")
    @PostMapping("/forget-password")
    public Result<String> forgetPassword(@Valid @RequestBody ForgetPasswordDTO dto) {
        log.info("忘记密码请求: phone={}", dto.getPhone());

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return Result.fail("两次输入的密码不一致");
        }
        if (!smsCodeUtil.verifySmsCode(dto.getPhone(), dto.getSmsCode(), "reset_password")) {
            return Result.fail("验证码错误或已失效");
        }

        User user = userCoreService.getByPhone(dto.getPhone());
        if (user == null) {
            return Result.fail("手机号未注册");
        }

        user.setPassword(PasswordUtil.encode(dto.getNewPassword()));
        user.setTokenVersion(normalizeTokenVersion(user) + 1);
        userCoreService.updateById(user);

        log.info("密码重置成功: phone={}", dto.getPhone());
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

    private int normalizeTokenVersion(User user) {
        return user == null || user.getTokenVersion() == null || user.getTokenVersion() < 0 ? 0 : user.getTokenVersion();
    }
}
