package com.huang.web.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huang.model.entity.Role;

/**
 * 角色服务接口
 * @author system
 * @since 2025-01-24
 */
public interface RoleService extends IService<Role> {

    /**
     * 根据角色编码查询角色
     * @param roleCode 角色编码
     * @return 角色信息
     */
    Role getByRoleCode(String roleCode);

    /**
     * 检查角色编码是否存在
     * @param roleCode 角色编码
     * @return 是否存在
     */
    boolean existsByRoleCode(String roleCode);
}