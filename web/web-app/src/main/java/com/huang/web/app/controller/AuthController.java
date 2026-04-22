package com.huang.web.app.controller;

import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
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
import com.huang.web.app.service.biz.auth.AppAuthCacheService;
import com.huang.web.app.service.core.RoleCoreService;
import com.huang.web.app.service.core.UserCoreService;
import com.huang.web.app.service.core.UserRoleCoreService;
import com.huang.web.app.vo.auth.LoginVO;
import com.huang.web.app.vo.auth.RefreshTokenVO;
import com.huang.web.app.vo.auth.RegisterVO;
import com.huang.web.app.vo.auth.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "App Auth", description = "Register, login, refresh token and password reset")
@Slf4j
@RestController
@RequestMapping("/app/auth")
@Validated
public class AuthController {

    private final SmsCodeUtil smsCodeUtil;
    private final UserCoreService userCoreService;
    private final RoleCoreService roleCoreService;
    private final UserRoleCoreService userRoleCoreService;
    private final AppAuthCacheService appAuthCacheService;
    private final CaptchaService captchaService;

    public AuthController(SmsCodeUtil smsCodeUtil,
                          UserCoreService userCoreService,
                          RoleCoreService roleCoreService,
                          UserRoleCoreService userRoleCoreService,
                          AppAuthCacheService appAuthCacheService,
                          CaptchaService captchaService) {
        this.smsCodeUtil = smsCodeUtil;
        this.userCoreService = userCoreService;
        this.roleCoreService = roleCoreService;
        this.userRoleCoreService = userRoleCoreService;
        this.appAuthCacheService = appAuthCacheService;
        this.captchaService = captchaService;
    }

    @Operation(summary = "Get slider captcha", description = "AJ-Captcha standard get endpoint")
    @PostMapping("/captcha/get")
    public ResponseModel getCaptcha(@RequestBody CaptchaVO captchaVO, HttpServletRequest request) {
        captchaVO.setBrowserInfo(clientIdentity(request));
        return captchaService.get(captchaVO);
    }

    @Operation(summary = "Check slider captcha", description = "AJ-Captcha standard check endpoint")
    @PostMapping("/captcha/check")
    public ResponseModel checkCaptcha(@RequestBody CaptchaVO captchaVO, HttpServletRequest request) {
        captchaVO.setBrowserInfo(clientIdentity(request));
        return captchaService.check(captchaVO);
    }

    @Operation(summary = "Send SMS code", description = "Send SMS code for register, login or password reset")
    @RateLimit(
            prefix = RedisConstant.APP_SMS_SEND_LIMIT_PREFIX,
            key = "#dto.type + ':' + #dto.phone",
            maxRequests = RedisConstant.SMS_SEND_RATE_LIMIT_MAX,
            windowSec = RedisConstant.SMS_SEND_RATE_LIMIT_WINDOW_SEC,
            message = "SMS requests are too frequent, please try again later"
    )
    @PostMapping("/sms-code/send")
    public Result<String> sendSmsCode(@Valid @RequestBody SmsCodeDTO dto) {
        log.info("send sms code request: phone={}, type={}", dto.getPhone(), dto.getType());

        if (!smsCodeUtil.canSendSms(dto.getPhone(), dto.getType())) {
            return Result.fail("SMS requests are too frequent, please try again later");
        }

        String code = smsCodeUtil.sendSmsCode(dto.getPhone(), dto.getType());
        if (code != null) {
            return Result.ok("SMS code sent successfully, dev code: " + code);
        }
        return Result.ok("SMS code sent successfully");
    }

    @Operation(summary = "Register user", description = "Register a new member")
    @PostMapping("/register")
    public Result<RegisterVO> register(@Valid @RequestBody UserRegisterDTO dto) {
        log.info("register request: username={}, phone={}", dto.getUsername(), dto.getPhone());

        if (!verifyCaptcha(dto.getCaptchaVerification())) {
            return Result.fail("Slider captcha is invalid or expired");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return Result.fail("Passwords do not match");
        }

        if (userCoreService.existsByUsername(dto.getUsername())) {
            return Result.fail("Username already exists");
        }
        if (userCoreService.existsByPhone(dto.getPhone())) {
            return Result.fail("Phone number already registered");
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank() && userCoreService.existsByEmail(dto.getEmail())) {
            return Result.fail("Email already registered");
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
        vo.setWelcomeMessage("Register success");

        log.info("register success: userId={}, username={}", vo.getUserId(), vo.getUsername());
        return Result.ok(vo);
    }

    @Operation(summary = "User login", description = "Password login with slider captcha")
    @RateLimit(
            prefix = RedisConstant.APP_LOGIN_LIMIT_IP_PREFIX,
            key = "#ip",
            maxRequests = RedisConstant.LOGIN_RATE_LIMIT_MAX,
            windowSec = RedisConstant.LOGIN_RATE_LIMIT_WINDOW_SEC,
            message = "Login requests are too frequent, please try again later"
    )
    @RateLimit(
            prefix = RedisConstant.APP_LOGIN_LIMIT_ACCOUNT_PREFIX,
            key = "#dto.account",
            maxRequests = RedisConstant.LOGIN_RATE_LIMIT_MAX,
            windowSec = RedisConstant.LOGIN_RATE_LIMIT_WINDOW_SEC,
            message = "Login requests are too frequent, please try again later"
    )
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody UserLoginDTO dto) {
        log.info("login request: account={}, loginType={}", dto.getAccount(), dto.getLoginType());

        if (!verifyCaptcha(dto.getCaptchaVerification())) {
            return Result.fail("Slider captcha is invalid or expired");
        }

        User user = findByAccount(dto.getAccount());
        if (user == null) {
            return Result.fail("Account not found");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            return Result.fail("Account disabled");
        }
        if ("password".equals(dto.getLoginType()) && !PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            return Result.fail("Invalid account or password");
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

        log.info("login success: userId={}, username={}", userInfo.getId(), userInfo.getUsername());
        return Result.ok(vo);
    }

    @Operation(summary = "Refresh token", description = "Use refresh token to get a new access token")
    @PostMapping("/refresh-token")
    public Result<RefreshTokenVO> refreshToken(@Valid @RequestBody RefreshTokenDTO dto) {
        log.info("refresh token request");

        var claims = JwtUtil.parseTokenSafely(dto.getRefreshToken());
        if (claims == null || !JwtUtil.isRefreshToken(claims)) {
            return Result.fail("Refresh token is invalid or expired");
        }
        Long userId = JwtUtil.getUserIdFromToken(dto.getRefreshToken());
        User user = userId == null ? null : userCoreService.getById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            return Result.fail("Refresh token is invalid or expired");
        }
        if (normalizeTokenVersion(user) != JwtUtil.getTokenVersionFromClaims(claims)) {
            return Result.fail("Refresh token is invalid or expired");
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

        log.info("refresh token success");
        return Result.ok(vo);
    }

    @Operation(summary = "Forget password", description = "Reset password using SMS code")
    @PostMapping("/forget-password")
    public Result<String> forgetPassword(@Valid @RequestBody ForgetPasswordDTO dto) {
        log.info("forget password request: phone={}", dto.getPhone());

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return Result.fail("Passwords do not match");
        }
        if (!smsCodeUtil.verifySmsCode(dto.getPhone(), dto.getSmsCode(), "reset_password")) {
            return Result.fail("SMS code is invalid or expired");
        }

        User user = userCoreService.getByPhone(dto.getPhone());
        if (user == null) {
            return Result.fail("Phone number is not registered");
        }

        user.setPassword(PasswordUtil.encode(dto.getNewPassword()));
        user.setTokenVersion(normalizeTokenVersion(user) + 1);
        boolean updated = userCoreService.updateById(user);
        if (!updated) {
            return Result.fail("Password reset failed");
        }
        appAuthCacheService.evict(user.getId());

        log.info("forget password success: phone={}", dto.getPhone());
        return Result.ok("Password reset success");
    }

    @Operation(summary = "Logout", description = "User logout")
    @PostMapping("/logout")
    public Result<String> logout() {
        log.info("logout success");
        return Result.ok("Logout success");
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

    private boolean verifyCaptcha(String captchaVerification) {
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaVerification(captchaVerification);
        ResponseModel responseModel = captchaService.verification(captchaVO);
        return responseModel != null && responseModel.isSuccess();
    }

    private String clientIdentity(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
