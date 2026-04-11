package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.constant.RedisConstant;
import com.huang.common.guard.RateLimit;
import com.huang.common.result.Result;
import com.huang.common.utils.JwtUtil;
import com.huang.common.utils.PasswordUtil;
import com.huang.model.entity.User;
import com.huang.web.admin.dto.auth.RefreshTokenDTO;
import com.huang.web.admin.vo.auth.AdminRefreshTokenVO;
import com.huang.web.admin.dto.auth.AdminLoginDTO;
import com.huang.web.admin.service.UserService;
import com.huang.web.admin.service.core.AdminRoleCoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.Map;

@Tag(name = "Admin Auth", description = "Admin login")
@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private final UserService userService;
    private final AdminRoleCoreService adminRoleCoreService;

    public AdminAuthController(UserService userService,
                               AdminRoleCoreService adminRoleCoreService) {
        this.userService = userService;
        this.adminRoleCoreService = adminRoleCoreService;
    }

    @Operation(summary = "Admin login")
    @RateLimit(
            prefix = RedisConstant.ADMIN_LOGIN_LIMIT_IP_PREFIX,
            key = "#ip",
            maxRequests = RedisConstant.LOGIN_RATE_LIMIT_MAX,
            windowSec = RedisConstant.LOGIN_RATE_LIMIT_WINDOW_SEC,
            message = "Login requests are too frequent, please try again later"
    )
    @RateLimit(
            prefix = RedisConstant.ADMIN_LOGIN_LIMIT_ACCOUNT_PREFIX,
            key = "#dto.account",
            maxRequests = RedisConstant.LOGIN_RATE_LIMIT_MAX,
            windowSec = RedisConstant.LOGIN_RATE_LIMIT_WINDOW_SEC,
            message = "Login requests are too frequent, please try again later"
    )
    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody AdminLoginDTO dto) {
        User user = userService.getOne(new LambdaQueryWrapper<User>()
                .and(w -> w.eq(User::getUsername, dto.getAccount()).or().eq(User::getPhone, dto.getAccount()))
                .last("LIMIT 1"));
        if (user == null) {
            return Result.fail("Account not found");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            return Result.fail("Account disabled");
        }
        if (!"admin".equalsIgnoreCase(user.getUserType())) {
            return Result.fail("Not an admin account");
        }
        if (!PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            return Result.fail("Invalid account or password");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("roleCodes", adminRoleCoreService.getRoleCodes(user.getId()));
        data.put("accessToken", JwtUtil.generateAdminAccessToken(user.getId(), user.getUsername(), normalizeTokenVersion(user)));
        data.put("refreshToken", JwtUtil.generateAdminRefreshToken(user.getId(), user.getUsername(), normalizeTokenVersion(user)));
        data.put("accessTokenExpire", LocalDateTime.now().plusSeconds(JwtUtil.accessTokenExpireMs(JwtUtil.PLATFORM_ADMIN) / 1000));
        data.put("refreshTokenExpire", LocalDateTime.now().plusSeconds(JwtUtil.refreshTokenExpireMs(JwtUtil.PLATFORM_ADMIN) / 1000));
        return Result.ok(data);
    }

    @Operation(summary = "Admin refresh token")
    @PostMapping("/refresh-token")
    public Result<AdminRefreshTokenVO> refreshToken(@Valid @RequestBody RefreshTokenDTO dto) {
        var claims = JwtUtil.parseTokenSafely(dto.getRefreshToken());
        if (claims == null || !JwtUtil.isRefreshToken(claims) || !JwtUtil.PLATFORM_ADMIN.equalsIgnoreCase(JwtUtil.getPlatformFromClaims(claims))) {
            return Result.fail("Refresh token is invalid or expired");
        }
        Long userId = JwtUtil.getUserIdFromToken(dto.getRefreshToken());
        User user = userId == null ? null : userService.getById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1 || !"admin".equalsIgnoreCase(user.getUserType())) {
            return Result.fail("Refresh token is invalid or expired");
        }
        if (normalizeTokenVersion(user) != JwtUtil.getTokenVersionFromClaims(claims)) {
            return Result.fail("Refresh token is invalid or expired");
        }

        AdminRefreshTokenVO vo = new AdminRefreshTokenVO();
        vo.setAccessToken(JwtUtil.generateAdminAccessToken(user.getId(), user.getUsername(), normalizeTokenVersion(user)));
        vo.setRefreshToken(dto.getRefreshToken());
        vo.setAccessTokenExpire(LocalDateTime.now().plusSeconds(JwtUtil.accessTokenExpireMs(JwtUtil.PLATFORM_ADMIN) / 1000));
        vo.setRefreshTokenExpire(LocalDateTime.now().plusSeconds(JwtUtil.refreshTokenExpireMs(JwtUtil.PLATFORM_ADMIN) / 1000));
        return Result.ok(vo);
    }

    private int normalizeTokenVersion(User user) {
        return user == null || user.getTokenVersion() == null || user.getTokenVersion() < 0 ? 0 : user.getTokenVersion();
    }
}
