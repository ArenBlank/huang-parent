package com.huang.web.app.controller;

import com.huang.common.login.LoginUserHolder;
import com.huang.common.result.Result;
import com.huang.common.utils.SmsCodeUtil;
import com.huang.web.app.dto.profile.*;
import com.huang.web.app.vo.profile.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * App端个人信息管理控制器
 * @author system
 * @since 2025-01-24
 */
@Tag(name = "App端个人信息管理", description = "个人资料查看、修改、头像上传、密码修改、账号注销等功能")
@Slf4j
@RestController
@RequestMapping("/app/profile")
@Validated
public class ProfileController {

    @Autowired
    private SmsCodeUtil smsCodeUtil;

    // TODO: 注入UserService等业务服务

    @Operation(summary = "获取个人信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/info")
    public Result<ProfileDetailVO> getProfileInfo() {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
        log.info("获取个人信息请求: 用户ID={}", currentUserId);
        
        // TODO: 从数据库查询用户详细信息
        
        // 临时返回模拟数据
        ProfileDetailVO vo = new ProfileDetailVO();
        vo.setId(currentUserId);
        vo.setUsername("testuser");
        vo.setNickname("测试用户");
        vo.setEmail("test@example.com");
        vo.setPhone("13888888888");
        vo.setAvatar("/uploads/avatar/default.jpg");
        vo.setGender(1);
        vo.setGenderText("男");
        vo.setBirthDate(LocalDate.of(1990, 1, 1));
        vo.setAge(34);
        vo.setBio("这是我的个人简介");
        vo.setAddress("北京市朝阳区");
        vo.setOccupation("软件工程师");
        vo.setHeight(175);
        vo.setWeight(70);
        vo.setStatus(1);
        vo.setVipLevel(0);
        vo.setCreateTime(LocalDateTime.now().minusDays(30));
        vo.setLastLoginTime(LocalDateTime.now());
        vo.setPoints(100);
        vo.setProfileCompleted(true);
        vo.setCompletionRate(85);
        
        log.info("获取个人信息成功: 用户ID={}", currentUserId);
        return Result.ok(vo);
    }

    @Operation(summary = "修改个人信息", description = "更新个人基本信息")
    @PutMapping("/info")
    public Result<String> updateProfile(@Valid @RequestBody ProfileUpdateDTO dto) {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
        log.info("修改个人信息请求: 用户ID={}, 昵称={}", currentUserId, dto.getNickname());
        
        // TODO: 实现个人信息更新逻辑
        // 1. 验证邮箱是否已被其他用户使用
        // 2. 更新用户信息
        // 3. 更新资料完成度
        
        log.info("个人信息修改成功: 用户ID={}", currentUserId);
        return Result.ok("个人信息修改成功");
    }

    @Operation(summary = "上传头像", description = "上传用户头像图片")
    @PostMapping("/avatar/upload")
    public Result<AvatarUploadVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
        log.info("上传头像请求: 用户ID={}, 文件名={}, 文件大小={}", 
                currentUserId, file.getOriginalFilename(), file.getSize());
        
        // 验证文件类型和大小
        if (file.isEmpty()) {
            return Result.fail("请选择要上传的文件");
        }
        
        if (file.getSize() > 5 * 1024 * 1024) { // 5MB
            return Result.fail("文件大小不能超过5MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.fail("只能上传图片文件");
        }
        
        // TODO: 实现文件上传逻辑
        // 1. 生成唯一文件名
        // 2. 上传到MinIO或本地存储
        // 3. 更新用户头像字段
        // 4. 删除旧头像文件（可选）
        
        // 临时返回模拟数据
        AvatarUploadVO vo = new AvatarUploadVO();
        vo.setAvatarUrl("/uploads/avatar/user_" + currentUserId + "_avatar.jpg");
        vo.setFileName(file.getOriginalFilename());
        vo.setFileSize(file.getSize());
        vo.setUploadTime(LocalDateTime.now());
        vo.setStatus("success");
        vo.setMessage("头像上传成功");
        
        log.info("头像上传成功: 用户ID={}, 文件路径={}", currentUserId, vo.getAvatarUrl());
        return Result.ok(vo);
    }

    @Operation(summary = "修改密码", description = "修改登录密码")
    @PutMapping("/password")
    public Result<String> updatePassword(@Valid @RequestBody PasswordUpdateDTO dto) {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
        log.info("修改密码请求: 用户ID={}", currentUserId);
        
        // 验证确认密码
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return Result.fail("两次输入的新密码不一致");
        }
        
        // 验证新密码不能与旧密码相同
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            return Result.fail("新密码不能与旧密码相同");
        }
        
        // TODO: 实现密码修改逻辑
        // 1. 验证旧密码是否正确
        // 2. 新密码加密
        // 3. 更新密码
        // 4. 清理相关令牌缓存
        
        log.info("密码修改成功: 用户ID={}", currentUserId);
        return Result.ok("密码修改成功");
    }

    @Operation(summary = "申请账号注销", description = "申请注销用户账号")
    @PostMapping("/cancel")
    public Result<AccountCancelVO> cancelAccount(@Valid @RequestBody AccountCancelDTO dto) {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
        log.info("账号注销申请: 用户ID={}, 注销类型={}, 原因={}", 
                currentUserId, dto.getCancelType(), dto.getReason());
        
        // 验证短信验证码
        // 注意：这里需要通过用户ID获取手机号来验证
        String userPhone = "13888888888"; // TODO: 从数据库获取用户手机号
        if (!smsCodeUtil.verifySmsCode(userPhone, dto.getSmsCode(), "cancel_account")) {
            return Result.fail("验证码错误或已失效");
        }
        
        // TODO: 实现账号注销申请逻辑
        // 1. 验证用户密码
        // 2. 创建注销申请记录
        // 3. 根据注销类型设置处理时间
        // 4. 发送通知（可选）
        
        // 临时返回模拟数据
        AccountCancelVO vo = new AccountCancelVO();
        vo.setCancelId(System.currentTimeMillis()); // 模拟申请ID
        vo.setUserId(currentUserId);
        vo.setCancelType(dto.getCancelType());
        vo.setStatus("pending");
        vo.setApplyTime(LocalDateTime.now());
        vo.setReason(dto.getReason());
        vo.setMessage("注销申请已提交，请等待处理");
        vo.setCancellable(true);
        
        if ("temporary".equals(dto.getCancelType())) {
            vo.setEffectiveTime(LocalDateTime.now().plusDays(7)); // 7天后生效
            vo.setCancelDeadline(LocalDateTime.now().plusDays(3)); // 3天内可撤销
        } else {
            vo.setEffectiveTime(LocalDateTime.now().plusDays(30)); // 30天后生效
            vo.setCancelDeadline(LocalDateTime.now().plusDays(15)); // 15天内可撤销
        }
        
        log.info("账号注销申请成功: 用户ID={}, 申请ID={}", currentUserId, vo.getCancelId());
        return Result.ok(vo);
    }

    @Operation(summary = "撤销注销申请", description = "撤销之前的账号注销申请")
    @PostMapping("/cancel/{cancelId}/revoke")
    public Result<String> revokeCancelApplication(@PathVariable Long cancelId) {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
        log.info("撤销注销申请: 用户ID={}, 申请ID={}", currentUserId, cancelId);
        
        // TODO: 实现撤销注销申请逻辑
        // 1. 查询注销申请记录
        // 2. 验证是否为当前用户的申请
        // 3. 检查是否在可撤销时间内
        // 4. 更新申请状态为已撤销
        
        log.info("注销申请撤销成功: 用户ID={}, 申请ID={}", currentUserId, cancelId);
        return Result.ok("注销申请已撤销");
    }

    @Operation(summary = "获取账号统计信息", description = "获取用户账号相关的统计数据")
    @GetMapping("/statistics")
    public Result<Object> getAccountStatistics() {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
        log.info("获取账号统计信息: 用户ID={}", currentUserId);
        
        // TODO: 实现统计信息查询
        // 1. 登录次数统计
        // 2. 活跃天数统计
        // 3. 功能使用统计等
        
        // 临时返回模拟数据
        return Result.ok("统计信息查询功能待实现");
    }
}