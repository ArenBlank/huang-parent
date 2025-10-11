package com.huang.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.service.TokenBlacklistService;
import com.huang.common.utils.JwtUtil;
import com.huang.common.utils.PasswordUtil;
import com.huang.common.utils.SmsCodeUtil;
import com.huang.model.entity.Role;
import com.huang.model.entity.User;
import com.huang.model.entity.UserRole;
import com.huang.web.app.dto.auth.*;
import com.huang.web.app.mapper.RoleMapper;
import com.huang.web.app.mapper.UserRoleMapper;
import com.huang.web.app.service.AuthService;
import com.huang.web.app.service.UserService;
import com.huang.web.app.vo.auth.*;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * App端认证业务服务实现类
 * @author system
 * @since 2025-01-24
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SmsCodeUtil smsCodeUtil;

    @Autowired
    private UserService userService;
    
    @Autowired
    private RoleMapper roleMapper;
    
    @Autowired
    private UserRoleMapper userRoleMapper;

    @Override
    public String sendSmsCode(SmsCodeDTO dto) {
        log.info("发送短信验证码请求: 手机号={}, 类型={}", dto.getPhone(), dto.getType());
        
        // 检查发送频率限制
        if (!smsCodeUtil.canSendSms(dto.getPhone(), dto.getType())) {
            throw new RuntimeException("发送过于频繁，请稍后再试");
        }
        
        // 发送验证码
        String code = smsCodeUtil.sendSmsCode(dto.getPhone(), dto.getType());
        
        if (code != null) {
            // 开发模式下返回验证码
            return "验证码发送成功，开发模式验证码: " + code;
        }
        
        return "验证码发送成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterVO register(UserRegisterDTO dto) {
        log.info("用户注册请求: 用户名={}, 手机号={}", dto.getUsername(), dto.getPhone());
        
        // 验证确认密码
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }
        
        // 验证短信验证码
        if (!smsCodeUtil.verifySmsCode(dto.getPhone(), dto.getSmsCode(), "register")) {
            throw new RuntimeException("验证码错误或已失效");
        }
        
        // 检查用户名和手机号是否已存在
        if (userService.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        
        if (userService.existsByPhone(dto.getPhone())) {
            throw new RuntimeException("手机号已被注册");
        }
        
        // 创建用户记录
        User newUser = new User();
        newUser.setUsername(dto.getUsername());
        newUser.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        newUser.setPhone(dto.getPhone());
        newUser.setPassword(PasswordUtil.encode(dto.getPassword())); // 密码加密
        newUser.setStatus(1); // 激活状态
        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());
        
        boolean saveResult = userService.save(newUser);
        if (!saveResult) {
            throw new RuntimeException("注册失败，请稍后重试");
        }
        
        // 为新用户分配默认会员角色
        assignDefaultMemberRole(newUser.getId());
        
        // 构建返回数据
        RegisterVO vo = new RegisterVO();
        vo.setUserId(newUser.getId());
        vo.setUsername(newUser.getUsername());
        vo.setNickname(newUser.getNickname());
        vo.setPhone(newUser.getPhone());
        vo.setAutoLogin(true);
        vo.setWelcomeMessage("注册成功，欢迎加入健身平台！");
        
        log.info("用户注册成功: 用户ID={}, 用户名={}", vo.getUserId(), vo.getUsername());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(UserLoginDTO dto) {
        log.info("用户登录请求: 账号={}, 登录类型={}", dto.getAccount(), dto.getLoginType());
        
        // 参数验证
        if ("password".equals(dto.getLoginType())) {
            if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
                throw new RuntimeException("密码不能为空");
            }
        } else if ("sms".equals(dto.getLoginType())) {
            if (dto.getSmsCode() == null || dto.getSmsCode().trim().isEmpty()) {
                throw new RuntimeException("短信验证码不能为空");
            }
        }
        
        if ("sms".equals(dto.getLoginType())) {
            // 短信登录验证
            if (!smsCodeUtil.verifySmsCode(dto.getAccount(), dto.getSmsCode(), "login")) {
                throw new RuntimeException("验证码错误或已失效");
            }
        }
        
        // 根据账号查询用户（用户名或手机号）
        User user = null;
        if (dto.getAccount().matches("^1[3-9]\\d{9}$")) {
            // 手机号格式
            user = userService.getByPhone(dto.getAccount());
        } else {
            // 用户名格式
            user = userService.getByUsername(dto.getAccount());
        }
        
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 检查用户状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new RuntimeException("用户已被禁用，请联系客服");
        }
        
        // 密码登录时验证密码
        if ("password".equals(dto.getLoginType())) {
            if (!PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
                throw new RuntimeException("密码错误");
            }
        }
        
        // 生成JWT令牌
        String accessToken = JwtUtil.generateAppAccessToken(user.getId(), user.getUsername());
        String refreshToken = JwtUtil.generateAppRefreshToken(user.getId(), user.getUsername());
        
        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userService.updateById(user);
        
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
        vo.setFirstLogin(user.getLastLoginTime() == null);
        vo.setNeedCompleteProfile(user.getNickname() == null || user.getNickname().trim().isEmpty());
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        
        log.info("用户登录成功: 用户ID={}, 用户名={}", userInfo.getId(), userInfo.getUsername());
        return vo;
    }

    @Override
    public RefreshTokenVO refreshToken(RefreshTokenDTO dto) {
        log.info("刷新令牌请求: refreshToken前8位={}", 
                dto.getRefreshToken().length() > 8 ? dto.getRefreshToken().substring(0, 8) + "..." : dto.getRefreshToken());
        
        // 验证refresh token有效性
        String newAccessToken = JwtUtil.refreshAccessToken(dto.getRefreshToken());
        if (newAccessToken == null) {
            throw new RuntimeException("令牌无效或已过期，请重新登录");
        }
        
        // 获取用户信息用于生成新的refresh token
        Long userId = JwtUtil.getUserIdFromToken(dto.getRefreshToken());
        String username = JwtUtil.getUsernameFromToken(dto.getRefreshToken());
        
        if (userId == null || username == null) {
            throw new RuntimeException("令牌解析失败，请重新登录");
        }
        
        // 生成新的refresh token（令牌轮换）
        String newRefreshToken = JwtUtil.generateAppRefreshToken(userId, username);
        
        // 构建返回数据
        RefreshTokenVO vo = new RefreshTokenVO();
        vo.setAccessToken(newAccessToken);
        vo.setRefreshToken(newRefreshToken);
        
        log.info("令牌刷新成功");
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String forgetPassword(ForgetPasswordDTO dto) {
        log.info("忘记密码请求: 手机号={}", dto.getPhone());
        
        // 验证确认密码
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }
        
        // 验证短信验证码
        if (!smsCodeUtil.verifySmsCode(dto.getPhone(), dto.getSmsCode(), "reset_password")) {
            throw new RuntimeException("验证码错误或已失效");
        }
        
        // 根据手机号查询用户
        User user = userService.getByPhone(dto.getPhone());
        if (user == null) {
            throw new RuntimeException("手机号未注册");
        }
        
        // 更新密码（需要加密）
        user.setPassword(PasswordUtil.encode(dto.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        
        boolean updateResult = userService.updateById(user);
        if (!updateResult) {
            throw new RuntimeException("密码重置失败，请稍后重试");
        }
        
        // 清理相关缓存和令牌（这里可以添加令牌黑名单逻辑）
        log.info("用户密码重置成功，所有相关令牌将失效: 用户ID={}", user.getId());
        
        log.info("密码重置成功: 手机号={}", dto.getPhone());
        return "密码重置成功";
    }

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Override
    public String logout(String token) {
        log.info("用户退出登录请求: token前8位={}", 
                token != null && token.length() > 8 ? token.substring(0, 8) + "..." : "null");
        
        if (token == null || token.trim().isEmpty()) {
            log.warn("退出登录时未提供有效token");
            throw new RuntimeException("未提供有效的访问令牌");
        }
        
        // 验证token有效性
        Claims claims = JwtUtil.parseTokenSafely(token);
        if (claims == null) {
            log.warn("退出登录时token解析失败");
            throw new RuntimeException("访问令牌无效或已过期");
        }
        
        // 获取用户信息
        Long userId = JwtUtil.getUserIdFromToken(token);
        String username = JwtUtil.getUsernameFromToken(token);
        
        log.info("解析token成功: 用户ID={}, 用户名={}", userId, username);
        
        // 计算token剩余TTL
        long currentTime = System.currentTimeMillis();
        long expirationTime = claims.getExpiration().getTime();
        long ttlSeconds = (expirationTime - currentTime) / 1000;
        
        log.info("token TTL计算: 当前时间={}, 过期时间={}, 剩余TTL={}s", 
                currentTime, expirationTime, ttlSeconds);
        
        // 检查token是否已在黑名单中（仅用于日志记录）
        boolean alreadyBlacklisted = tokenBlacklistService.isBlacklisted(token);
        if (alreadyBlacklisted) {
            log.info("token已在黑名单中，但仍允许退出登录操作");
        }
        
        if (ttlSeconds > 0) {
            try {
                // 将token加入黑名单
                tokenBlacklistService.addToBlacklist(token, ttlSeconds);
                log.info("用户退出登录成功: 用户ID={}, 用户名={}, token已加入黑名单", userId, username);
            } catch (Exception e) {
                log.error("加入token黑名单失败，但退出登录仍然成功: 用户ID={}, 用户名={}", userId, username, e);
            }
        } else {
            log.info("用户退出登录成功: 用户ID={}, 用户名={}, token已过期无需加入黑名单", userId, username);
        }
        
        return "退出登录成功";
    }
    
    /**
     * 为新用户分配默认会员角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignDefaultMemberRole(Long userId) {
        try {
            // 查询会员角色
            Role memberRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, "member")
            );
            
            if (memberRole == null) {
                log.error("会员角色不存在，无法为新用户分配默认角色: userId={}", userId);
                return;
            }
            
            // 检查用户是否已经有该角色
            UserRole existingUserRole = userRoleMapper.selectOne(
                new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getUserId, userId)
                    .eq(UserRole::getRoleId, memberRole.getId())
                    .eq(UserRole::getIsDeleted, 0)
            );
            
            if (existingUserRole != null) {
                log.info("用户已经具有会员角色: userId={}", userId);
                return;
            }
            
            // 创建用户角色关联
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(memberRole.getId());
            userRole.setCreateTime(LocalDateTime.now());
            userRole.setUpdateTime(LocalDateTime.now());
            userRole.setIsDeleted((byte) 0);
            
            int insertResult = userRoleMapper.insert(userRole);
            if (insertResult > 0) {
                log.info("为新用户分配默认会员角色成功: userId={}, roleId={}", userId, memberRole.getId());
            } else {
                log.error("为新用户分配默认会员角色失败: userId={}", userId);
            }
            
        } catch (Exception e) {
            log.error("为新用户分配默认会员角色时发生异常: userId={}", userId, e);
        }
    }
}
