package com.huang.web.admin.service.biz.auth;

import com.huang.model.entity.Role;
import com.huang.model.entity.User;
import com.huang.web.admin.custom.config.AdminPermissionProperties;
import com.huang.web.admin.mapper.AdminAuthViewQueryMapper;
import com.huang.web.admin.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class AdminAuthViewLoader {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthViewLoader.class);

    private final UserService userService;
    private final AdminAuthViewQueryMapper adminAuthViewQueryMapper;
    private final AdminPermissionProperties adminPermissionProperties;

    public AdminAuthViewLoader(UserService userService,
                               AdminAuthViewQueryMapper adminAuthViewQueryMapper,
                               AdminPermissionProperties adminPermissionProperties) {
        this.userService = userService;
        this.adminAuthViewQueryMapper = adminAuthViewQueryMapper;
        this.adminPermissionProperties = adminPermissionProperties;
    }

    public AdminAuthView load(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userService.getById(userId);
        if (user == null) {
            log.debug("Admin auth view load skipped because user {} was not found", userId);
            return null;
        }

        List<Role> roles = adminAuthViewQueryMapper.selectActiveRolesByUserId(userId);
        LinkedHashSet<Long> roleIds = new LinkedHashSet<>();
        LinkedHashSet<String> roleCodes = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(roles)) {
            for (Role role : roles) {
                if (role == null || role.getId() == null || !StringUtils.hasText(role.getRoleCode())) {
                    continue;
                }
                roleIds.add(role.getId());
                roleCodes.add(role.getRoleCode().trim());
            }
        }

        LinkedHashSet<String> permissionCodes = new LinkedHashSet<>(sanitizeStrings(
                adminAuthViewQueryMapper.selectPermissionCodesByRoleIds(roleIds)));
        LinkedHashSet<Long> categoryIds = new LinkedHashSet<>(sanitizeLongs(
                adminAuthViewQueryMapper.selectCategoryIdsByRoleIds(roleIds)));

        for (String roleCode : roleCodes) {
            permissionCodes.addAll(sanitizeStrings(adminPermissionProperties.permissionsFor(roleCode)));
            categoryIds.addAll(sanitizeLongs(adminPermissionProperties.courseCategoriesFor(roleCode)));
        }

        boolean adminAll = adminPermissionProperties.isAdminAll() && roleCodes.contains("ADMIN");
        return new AdminAuthView(
                user.getId(),
                user.getUsername(),
                user.getUserType(),
                user.getStatus(),
                normalizeTokenVersion(user.getTokenVersion()),
                roleIds,
                roleCodes,
                permissionCodes,
                categoryIds,
                adminAll
        );
    }

    private int normalizeTokenVersion(Integer tokenVersion) {
        return tokenVersion == null || tokenVersion < 0 ? 0 : tokenVersion;
    }

    private LinkedHashSet<String> sanitizeStrings(Collection<String> values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (CollectionUtils.isEmpty(values)) {
            return result;
        }
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            result.add(value.trim());
        }
        return result;
    }

    private LinkedHashSet<Long> sanitizeLongs(Collection<Long> values) {
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        if (CollectionUtils.isEmpty(values)) {
            return result;
        }
        values.stream()
                .filter(Objects::nonNull)
                .forEach(result::add);
        return result;
    }
}
