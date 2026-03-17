package com.huang.web.app.controller;

import com.huang.common.login.LoginUserHolder;
import com.huang.common.result.Result;
import com.huang.common.utils.PasswordUtil;
import com.huang.model.entity.User;
import com.huang.model.entity.UserProfile;
import com.huang.web.app.dto.profile.PasswordUpdateDTO;
import com.huang.web.app.dto.profile.ProfileUpdateDTO;
import com.huang.web.app.service.core.UserCoreService;
import com.huang.web.app.service.core.UserProfileCoreService;
import com.huang.web.app.vo.profile.AvatarUploadVO;
import com.huang.web.app.vo.profile.ProfileDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

@Tag(name = "App Profile", description = "Profile and account settings")
@Slf4j
@RestController
@RequestMapping("/app/profile")
@Validated
public class ProfileController {

    private final UserCoreService userCoreService;
    private final UserProfileCoreService userProfileCoreService;

    public ProfileController(UserCoreService userCoreService, UserProfileCoreService userProfileCoreService) {
        this.userCoreService = userCoreService;
        this.userProfileCoreService = userProfileCoreService;
    }

    @Operation(summary = "Get profile info")
    @GetMapping("/info")
    public Result<ProfileDetailVO> getProfileInfo() {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
        User user = userCoreService.getById(currentUserId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        UserProfile profile = userProfileCoreService.getByUserId(currentUserId);

        ProfileDetailVO vo = new ProfileDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setGenderText(resolveGenderText(user.getGender()));
        vo.setBirthDate(user.getBirthDate());
        vo.setAge(resolveAge(user.getBirthDate()));
        vo.setStatus(user.getStatus());
        vo.setVipLevel(0);
        if (user.getCreateTime() != null) {
            vo.setCreateTime(user.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        if (profile != null) {
            vo.setBio(profile.getBio());
            vo.setAddress(profile.getAddress());
            vo.setOccupation(profile.getOccupation());
            vo.setHeight(profile.getHeight());
            vo.setWeight(profile.getWeight());
        }
        vo.setProfileCompleted(isProfileCompleted(user, profile));
        vo.setCompletionRate(calculateCompletion(user, profile));
        return Result.ok(vo);
    }

    @Operation(summary = "Update profile")
    @PutMapping("/info")
    public Result<String> updateProfile(@Valid @RequestBody ProfileUpdateDTO dto) {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
        log.info("update profile userId={}, nickname={}", currentUserId, dto.getNickname());
        User user = userCoreService.getById(currentUserId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        if (dto.getBirthDate() != null) {
            user.setBirthDate(dto.getBirthDate());
        }
        userCoreService.updateById(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(currentUserId);
        profile.setBio(dto.getBio());
        profile.setAddress(dto.getAddress());
        profile.setOccupation(dto.getOccupation());
        profile.setHeight(dto.getHeight());
        profile.setWeight(dto.getWeight());
        userProfileCoreService.upsertProfile(profile);

        return Result.ok("个人信息修改成功");
    }

    @Operation(summary = "Upload avatar")
    @PostMapping("/avatar/upload")
    public Result<AvatarUploadVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail("请选择要上传的文件");
        }
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
        String avatarUrl = "/uploads/avatar/user_" + currentUserId + "_avatar.jpg";
        User user = userCoreService.getById(currentUserId);
        if (user != null) {
            user.setAvatar(avatarUrl);
            userCoreService.updateById(user);
        }
        AvatarUploadVO vo = new AvatarUploadVO();
        vo.setAvatarUrl(avatarUrl);
        vo.setFileName(file.getOriginalFilename());
        vo.setFileSize(file.getSize());
        vo.setStatus("success");
        vo.setMessage("头像上传成功");
        return Result.ok(vo);
    }

    @Operation(summary = "Update password")
    @PutMapping("/password")
    public Result<String> updatePassword(@Valid @RequestBody PasswordUpdateDTO dto) {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return Result.fail("两次输入的新密码不一致");
        }
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            return Result.fail("新密码不能与旧密码相同");
        }
        User user = userCoreService.getById(currentUserId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        if (!PasswordUtil.matches(dto.getOldPassword(), user.getPassword())) {
            return Result.fail("旧密码错误");
        }
        user.setPassword(PasswordUtil.encode(dto.getNewPassword()));
        userCoreService.updateById(user);
        return Result.ok("密码修改成功");
    }

    private String resolveGenderText(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        if (gender == 1) {
            return "男";
        }
        if (gender == 2) {
            return "女";
        }
        return "未知";
    }

    private Integer resolveAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private boolean isProfileCompleted(User user, UserProfile profile) {
        if (user == null) {
            return false;
        }
        return user.getNickname() != null
                && user.getEmail() != null
                && user.getPhone() != null
                && profile != null
                && profile.getAddress() != null;
    }

    private int calculateCompletion(User user, UserProfile profile) {
        int total = 5;
        int score = 0;
        if (user != null) {
            if (user.getNickname() != null) score++;
            if (user.getEmail() != null) score++;
            if (user.getPhone() != null) score++;
            if (user.getBirthDate() != null) score++;
        }
        if (profile != null && profile.getAddress() != null) score++;
        return (int) Math.round(score * 100.0 / total);
    }
}
