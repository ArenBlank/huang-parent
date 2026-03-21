package com.huang.web.app.controller;

import com.huang.common.login.LoginUserHolder;
import com.huang.common.minio.MinioProperties;
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
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Tag(name = "App Profile", description = "Profile and account settings")
@Slf4j
@RestController
@RequestMapping("/app/profile")
@Validated
public class ProfileController {

    private final UserCoreService userCoreService;
    private final UserProfileCoreService userProfileCoreService;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public ProfileController(UserCoreService userCoreService,
                             UserProfileCoreService userProfileCoreService,
                             ObjectProvider<MinioClient> minioClientProvider,
                             MinioProperties minioProperties) {
        this.userCoreService = userCoreService;
        this.userProfileCoreService = userProfileCoreService;
        this.minioClient = minioClientProvider.getIfAvailable();
        this.minioProperties = minioProperties;
    }

    @Operation(summary = "Get profile info")
    @GetMapping("/info")
    public Result<ProfileDetailVO> getProfileInfo() {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
        User user = userCoreService.getById(currentUserId);
        if (user == null) {
            return Result.fail("User not found");
        }
        UserProfile profile = userProfileCoreService.getByUserId(currentUserId);

        ProfileDetailVO vo = new ProfileDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(resolveAvatarUrl(user.getAvatar()));
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
            return Result.fail("User not found");
        }
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
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

        return Result.ok("Profile updated");
    }

    @Operation(summary = "Upload avatar")
    @PostMapping("/avatar/upload")
    public Result<AvatarUploadVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail("Please choose a file");
        }
        if (!isImageFile(file)) {
            return Result.fail("Only image files are supported");
        }
        if (minioClient == null || !StringUtils.hasText(minioProperties.getBucketName())) {
            return Result.fail("File storage is not configured");
        }

        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
        String objectPath = buildAvatarObjectPath(currentUserId, file.getOriginalFilename());

        try {
            ensureBucketExists();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectPath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(resolveContentType(file))
                            .build()
            );

            User user = userCoreService.getById(currentUserId);
            if (user != null) {
                user.setAvatar(objectPath);
                userCoreService.updateById(user);
            }

            AvatarUploadVO vo = new AvatarUploadVO();
            vo.setAvatarUrl(resolveAvatarUrl(objectPath));
            vo.setFileName(file.getOriginalFilename());
            vo.setFileSize(file.getSize());
            vo.setStatus("success");
            vo.setMessage("Avatar uploaded");
            return Result.ok(vo);
        } catch (Exception e) {
            log.error("upload avatar failed, userId={}", currentUserId, e);
            return Result.fail("Avatar upload failed");
        }
    }

    @Operation(summary = "Update password")
    @PutMapping("/password")
    public Result<String> updatePassword(@Valid @RequestBody PasswordUpdateDTO dto) {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return Result.fail("Passwords do not match");
        }
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            return Result.fail("New password must be different from old password");
        }
        User user = userCoreService.getById(currentUserId);
        if (user == null) {
            return Result.fail("User not found");
        }
        if (!PasswordUtil.matches(dto.getOldPassword(), user.getPassword())) {
            return Result.fail("Old password is incorrect");
        }
        user.setPassword(PasswordUtil.encode(dto.getNewPassword()));
        userCoreService.updateById(user);
        return Result.ok("Password updated");
    }

    private String resolveGenderText(Integer gender) {
        if (gender == null) {
            return "Unknown";
        }
        if (gender == 1) {
            return "Male";
        }
        if (gender == 2) {
            return "Female";
        }
        return "Unknown";
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

    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && contentType.toLowerCase().startsWith("image/")) {
            return true;
        }
        String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        return ".jpg".equals(extension)
                || ".jpeg".equals(extension)
                || ".png".equals(extension)
                || ".webp".equals(extension)
                || ".gif".equals(extension);
    }

    private String buildAvatarObjectPath(Long userId, String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String datePath = LocalDate.now().toString().replace("-", "");
        return "avatar/upload/" + datePath + "/user_" + userId + "_" + UUID.randomUUID().toString().replace("-", "") + extension;
    }

    private String getFileExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return ".jpg";
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    private String resolveContentType(MultipartFile file) {
        return StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream";
    }

    private void ensureBucketExists() throws Exception {
        String bucket = minioProperties.getBucketName();
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private String resolveAvatarUrl(String avatar) {
        if (!StringUtils.hasText(avatar)) {
            return avatar;
        }
        if (avatar.startsWith("http://") || avatar.startsWith("https://") || avatar.startsWith("/")) {
            return rewritePublicUrl(avatar);
        }
        if (minioClient != null && StringUtils.hasText(minioProperties.getBucketName())) {
            try {
                return rewritePublicUrl(minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(minioProperties.getBucketName())
                                .object(avatar)
                                .expiry(7, TimeUnit.DAYS)
                                .build()
                ));
            } catch (Exception e) {
                log.warn("resolve avatar url failed, avatar={}", avatar, e);
            }
        }
        String endpoint = StringUtils.hasText(minioProperties.getPublicEndpoint())
                ? minioProperties.getPublicEndpoint()
                : "http://files.localhost";
        if (StringUtils.hasText(endpoint) && StringUtils.hasText(minioProperties.getBucketName())) {
            String safeEndpoint = endpoint.endsWith("/")
                    ? endpoint.substring(0, endpoint.length() - 1)
                    : endpoint;
            return safeEndpoint + "/" + minioProperties.getBucketName() + "/" + avatar;
        }
        return avatar;
    }

    private String rewritePublicUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        String publicEndpoint = StringUtils.hasText(minioProperties.getPublicEndpoint())
                ? minioProperties.getPublicEndpoint()
                : "http://files.localhost";
        String internalEndpoint = StringUtils.hasText(minioProperties.getEndpoint())
                ? minioProperties.getEndpoint()
                : "http://localhost:9000";
        String safePublic = publicEndpoint.endsWith("/")
                ? publicEndpoint.substring(0, publicEndpoint.length() - 1)
                : publicEndpoint;
        String safeInternal = internalEndpoint.endsWith("/")
                ? internalEndpoint.substring(0, internalEndpoint.length() - 1)
                : internalEndpoint;
        return url.replace(safeInternal, safePublic).replace("http://localhost:9000", safePublic);
    }
}
