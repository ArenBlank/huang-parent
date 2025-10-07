package com.huang.web.app.controller;

import com.huang.common.result.Result;
import com.huang.common.utils.JwtUtil;
import com.huang.common.utils.PasswordUtil;
import com.huang.model.entity.User;
import com.huang.web.app.dto.auth.UserLoginDTO;
import com.huang.web.app.service.UserService;
import com.huang.web.app.vo.auth.LoginVO;
import com.huang.web.app.vo.auth.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;

/**
 * App端简化认证控制器 - 用于测试登录功能
 * @author system
 * @since 2025-01-24
 */
@Tag(name = "App端简化认证管理", description = "用于测试的简化登录功能")
@Slf4j
@RestController
@RequestMapping("/app/auth")
@Validated
public class SimpleAuthController {

    @Autowired
    private UserService userService;

    @Operation(summary = "用户登录", description = "密码登录")
    @PostMapping("/simple-login")
    public Result<LoginVO> login(@Valid @RequestBody UserLoginDTO dto) {
        log.info("用户登录请求: 账号={}, 登录类型={}", dto.getAccount(), dto.getLoginType());
        
        // 1. 根据账号查询用户（用户名或手机号）
        User user = null;
        if (dto.getAccount().matches("^1[3-9]\\d{9}$")) {
            // 手机号格式
            user = userService.getByPhone(dto.getAccount());
        } else {
            // 用户名格式
            user = userService.getByUsername(dto.getAccount());
        }
        
        if (user == null) {
            return Result.fail("用户不存在");
        }
        
        // 2. 检查用户状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            return Result.fail("用户已被禁用，请联系客服");
        }
        
        // 3. 验证密码
        if (!PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            return Result.fail("密码错误");
        }
        
        // 4. 生成JWT令牌
        String accessToken = JwtUtil.generateAppAccessToken(user.getId(), user.getUsername());
        String refreshToken = JwtUtil.generateAppRefreshToken(user.getId(), user.getUsername());
        
        // 5. 跳过更新最后登录时间 - 简化处理
        
        // 构建返回数据
        LoginVO vo = new LoginVO();
        
        // 用户信息
        UserInfoVO userInfo = new UserInfoVO();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setPhone(user.getPhone());
        userInfo.setGender(user.getGender());
        userInfo.setStatus(user.getStatus());
        
        vo.setUserInfo(userInfo);
        vo.setFirstLogin(false); // 简化处理
        vo.setNeedCompleteProfile(false); // 简化处理
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        
        log.info("用户登录成功: 用户ID={}, 用户名={}", userInfo.getId(), userInfo.getUsername());
        return Result.ok(vo);
    }
}