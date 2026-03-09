package com.huang.web.admin.service.core.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.model.entity.Role;
import com.huang.model.entity.UserRole;
import com.huang.web.admin.service.RoleService;
import com.huang.web.admin.service.UserRoleService;
import com.huang.web.admin.service.core.AdminRoleCoreService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminRoleCoreServiceImpl implements AdminRoleCoreService {

    private final UserRoleService userRoleService;
    private final RoleService roleService;

    public AdminRoleCoreServiceImpl(UserRoleService userRoleService, RoleService roleService) {
        this.userRoleService = userRoleService;
        this.roleService = roleService;
    }

    @Override
    public Set<String> getRoleCodes(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        List<Long> roleIds = userRoleService.list(new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId))
                .stream()
                .map(UserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        return roleService.listByIds(roleIds).stream()
                .filter(role -> role.getStatus() != null && role.getStatus() == 1)
                .map(Role::getRoleCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
