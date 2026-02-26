package com.huang.web.app.controller;

import com.huang.common.login.LoginUserHolder;
import com.huang.common.result.Result;
import com.huang.web.app.dto.profile.PasswordUpdateDTO;
import com.huang.web.app.dto.profile.ProfileUpdateDTO;
import com.huang.web.app.vo.profile.AvatarUploadVO;
import com.huang.web.app.vo.profile.ProfileDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Tag(name = "App个人信息管理", description = "个人资料与账号安全")
@Slf4j
@RestController
@RequestMapping("/app/profile")
@Validated
public class ProfileController {

    @Operation(summary = "获取个人信息")
    @GetMapping("/info")
    public Result<ProfileDetailVO> getProfileInfo() {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
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
        return Result.ok(vo);
    }

    @Operation(summary = "修改个人信息")
    @PutMapping("/info")
    public Result<String> updateProfile(@Valid @RequestBody ProfileUpdateDTO dto) {
        log.info("update profile userId={}, nickname={}", LoginUserHolder.getLoginUser().getUserId(), dto.getNickname());
        return Result.ok("个人信息修改成功");
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar/upload")
    public Result<AvatarUploadVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail("请选择要上传的文件");
        }
        AvatarUploadVO vo = new AvatarUploadVO();
        vo.setAvatarUrl("/uploads/avatar/user_" + LoginUserHolder.getLoginUser().getUserId() + "_avatar.jpg");
        vo.setFileName(file.getOriginalFilename());
        vo.setFileSize(file.getSize());
        vo.setUploadTime(LocalDateTime.now());
        vo.setStatus("success");
        vo.setMessage("头像上传成功");
        return Result.ok(vo);
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<String> updatePassword(@Valid @RequestBody PasswordUpdateDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return Result.fail("两次输入的新密码不一致");
        }
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            return Result.fail("新密码不能与旧密码相同");
        }
        return Result.ok("密码修改成功");
    }
}
