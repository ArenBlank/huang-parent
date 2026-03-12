package com.huang.web.admin.service.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.model.entity.Permission;
import com.huang.model.entity.Role;
import com.huang.model.entity.RolePermission;
import com.huang.web.admin.mapper.PermissionMapper;
import com.huang.web.admin.mapper.RolePermissionMapper;
import com.huang.web.admin.service.RoleService;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolve admin permissions from database role-permission mappings.
 */
@Service
public class AdminPermissionDbService {

    private final RoleService roleService;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    public AdminPermissionDbService(RoleService roleService,
                                    RolePermissionMapper rolePermissionMapper,
                                    PermissionMapper permissionMapper) {
        this.roleService = roleService;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
    }

    public Set<String> permissionsForRoleCodes(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return new HashSet<>();
        }
        List<Role> roles = roleService.list(new LambdaQueryWrapper<Role>()
                .in(Role::getRoleCode, roleCodes));
        if (roles == null || roles.isEmpty()) {
            return new HashSet<>();
        }
        Set<Long> roleIds = roles.stream()
                .map(Role::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return new HashSet<>();
        }
        List<RolePermission> links = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds));
        if (links == null || links.isEmpty()) {
            return new HashSet<>();
        }
        Set<Long> permIds = links.stream()
                .map(RolePermission::getPermId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (permIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Permission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>()
                        .in(Permission::getId, permIds)
                        .eq(Permission::getStatus, 1));
        if (permissions == null || permissions.isEmpty()) {
            return new HashSet<>();
        }
        return permissions.stream()
                .map(Permission::getPermCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
