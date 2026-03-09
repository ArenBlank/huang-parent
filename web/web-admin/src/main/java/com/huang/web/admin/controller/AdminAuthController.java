package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.result.Result;
import com.huang.common.utils.JwtUtil;
import com.huang.common.utils.PasswordUtil;
import com.huang.model.entity.User;
import com.huang.web.admin.dto.auth.AdminLoginDTO;
import com.huang.web.admin.service.core.AdminRoleCoreService;
import com.huang.web.admin.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Admin Auth", description = "Admin login")
@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private final UserService userService;
    private final AdminRoleCoreService adminRoleCoreService;

    public AdminAuthController(UserService userService, AdminRoleCoreService adminRoleCoreService) {
        this.userService = userService;
        this.adminRoleCoreService = adminRoleCoreService;
    }

    @Operation(summary = "Admin login")
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
        data.put("accessToken", JwtUtil.generateAdminAccessToken(user.getId(), user.getUsername()));
        data.put("refreshToken", JwtUtil.generateAdminRefreshToken(user.getId(), user.getUsername()));
        return Result.ok(data);
    }
}
