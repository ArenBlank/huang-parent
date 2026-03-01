package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.huang.model.entity.CoachProfile;
import com.huang.model.entity.Role;
import com.huang.model.entity.User;
import com.huang.model.entity.UserRole;
import com.huang.web.admin.dto.coach.CoachApplyAuditDTO;
import com.huang.web.admin.mapper.CoachProfileMapper;
import com.huang.web.admin.service.RoleService;
import com.huang.web.admin.service.UserRoleService;
import com.huang.web.admin.service.UserService;
import com.huang.web.admin.vo.coach.AdminCoachApplyVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminCoachApplyBizService {

    private static final int CERT_PENDING = 0;
    private static final int CERT_APPROVED = 1;
    private static final int CERT_REJECTED = 2;
    private static final SimpleDateFormat DATETIME_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final CoachProfileMapper coachProfileMapper;
    private final UserService userService;
    private final RoleService roleService;
    private final UserRoleService userRoleService;

    public AdminCoachApplyBizService(CoachProfileMapper coachProfileMapper,
                                     UserService userService,
                                     RoleService roleService,
                                     UserRoleService userRoleService) {
        this.coachProfileMapper = coachProfileMapper;
        this.userService = userService;
        this.roleService = roleService;
        this.userRoleService = userRoleService;
    }

    public List<AdminCoachApplyVO> list(Integer certStatus, String keyword) {
        LambdaQueryWrapper<CoachProfile> wrapper = new LambdaQueryWrapper<CoachProfile>()
                .orderByDesc(CoachProfile::getUpdateTime);
        if (certStatus != null) {
            wrapper.eq(CoachProfile::getCertStatus, certStatus);
        }
        List<CoachProfile> profiles = coachProfileMapper.selectList(wrapper);
        List<AdminCoachApplyVO> result = new ArrayList<>();
        for (CoachProfile profile : profiles) {
            User user = userService.getById(profile.getUserId());
            if (user == null) {
                continue;
            }
            if (StringUtils.hasText(keyword)
                    && !containsIgnoreCase(user.getUsername(), keyword)
                    && !containsIgnoreCase(user.getNickname(), keyword)
                    && !containsIgnoreCase(user.getPhone(), keyword)) {
                continue;
            }
            result.add(toVO(profile, user));
        }
        return result;
    }

    public AdminCoachApplyVO detail(Long profileId) {
        CoachProfile profile = coachProfileMapper.selectById(profileId);
        if (profile == null) {
            return null;
        }
        User user = userService.getById(profile.getUserId());
        if (user == null) {
            return null;
        }
        return toVO(profile, user);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean audit(CoachApplyAuditDTO dto) {
        if (dto.getCertStatus() == null || (dto.getCertStatus() != CERT_APPROVED && dto.getCertStatus() != CERT_REJECTED)) {
            return false;
        }

        CoachProfile profile = coachProfileMapper.selectById(dto.getProfileId());
        if (profile == null) {
            return false;
        }
        User user = userService.getById(profile.getUserId());
        if (user == null) {
            return false;
        }

        int updatedRows = coachProfileMapper.update(null, new LambdaUpdateWrapper<CoachProfile>()
                .eq(CoachProfile::getId, profile.getId())
                .eq(CoachProfile::getCertStatus, CERT_PENDING)
                .set(CoachProfile::getCertStatus, dto.getCertStatus()));
        if (updatedRows == 0) {
            return false;
        }

        if (dto.getCertStatus() == CERT_APPROVED) {
            user.setUserType("coach");
            userService.updateById(user);
            ensureCoachRole(user.getId());
        }
        return true;
    }

    private void ensureCoachRole(Long userId) {
        Role coachRole = roleService.getByRoleCode("COACH");
        if (coachRole == null) {
            return;
        }
        UserRole exists = userRoleService.getOne(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, coachRole.getId())
                .last("LIMIT 1"));
        if (exists != null) {
            return;
        }
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(coachRole.getId());
        userRoleService.save(userRole);
    }

    private AdminCoachApplyVO toVO(CoachProfile profile, User user) {
        AdminCoachApplyVO vo = new AdminCoachApplyVO();
        vo.setProfileId(profile.getId());
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setBio(profile.getBio());
        vo.setExpertise(profile.getExpertise());
        vo.setYears(profile.getYears());
        vo.setPrice(profile.getPrice());
        vo.setCertStatus(profile.getCertStatus());
        vo.setCertStatusText(toCertStatusText(profile.getCertStatus()));
        vo.setStatus(profile.getStatus());
        vo.setUpdateTime(profile.getUpdateTime() == null ? null : DATETIME_FMT.format(profile.getUpdateTime()));
        return vo;
    }

    private String toCertStatusText(Integer certStatus) {
        if (certStatus == null) {
            return "UNKNOWN";
        }
        return switch (certStatus) {
            case CERT_PENDING -> "PENDING";
            case CERT_APPROVED -> "APPROVED";
            case CERT_REJECTED -> "REJECTED";
            default -> "UNKNOWN";
        };
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }
}
