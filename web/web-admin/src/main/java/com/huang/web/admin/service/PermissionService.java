package com.huang.web.admin.service;

import com.huang.model.entity.Permission;

import java.util.List;

/**
 * 权限服务接口(模拟实现)
 */
public interface PermissionService {

    /**
     * 根据角色ID查询权限列表
     */
    List<Permission> getPermissionsByRoleId(Long roleId);

    /**
     * 根据用户ID查询权限列表
     */
    List<Permission> getPermissionsByUserId(Long userId);

    /**
     * 检查用户是否有指定权限
     */
    boolean hasPermission(Long userId, String permissionCode);

    /**
     * 检查用户是否有访问资源的权限
     */
    boolean hasResourcePermission(Long userId, String resourcePath, String method);
}
