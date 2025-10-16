package com.huang.web.admin.service.impl;

import com.huang.model.entity.Permission;
import com.huang.web.admin.mapper.PermissionMapper;
import com.huang.web.admin.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限服务实现类(模拟实现)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;

    @Override
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        return permissionMapper.selectByRoleId(roleId);
    }

    @Override
    public List<Permission> getPermissionsByUserId(Long userId) {
        return permissionMapper.selectByUserId(userId);
    }

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        // 模拟实现:管理员(userId=1)拥有所有权限
        if (userId != null && userId == 1L) {
            return true;
        }

        List<Permission> permissions = getPermissionsByUserId(userId);
        return permissions.stream()
            .anyMatch(p -> permissionCode.equals(p.getPermissionCode()));
    }

    @Override
    public boolean hasResourcePermission(Long userId, String resourcePath, String method) {
        // 模拟实现:管理员(userId=1)可以访问所有资源
        if (userId != null && userId == 1L) {
            return true;
        }

        List<Permission> permissions = getPermissionsByUserId(userId);
        return permissions.stream()
            .anyMatch(p -> {
                // 匹配资源路径和请求方法
                boolean pathMatches = resourcePath.matches(p.getResourcePath().replace("*", ".*"));
                boolean methodMatches = "ALL".equals(p.getResourceMethod()) ||
                    method.equalsIgnoreCase(p.getResourceMethod());
                return pathMatches && methodMatches;
            });
    }
}
