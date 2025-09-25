package com.huang.web.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.model.entity.Role;
import com.huang.web.admin.mapper.RoleMapper;
import com.huang.web.admin.service.RoleService;
import org.springframework.stereotype.Service;

/**
 * 角色服务实现类
 * @author system
 * @since 2025-01-24
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Override
    public Role getByRoleCode(String roleCode) {
        if (roleCode == null || roleCode.trim().isEmpty()) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, roleCode)
                .last("LIMIT 1"));
    }

    @Override
    public boolean existsByRoleCode(String roleCode) {
        if (roleCode == null || roleCode.trim().isEmpty()) {
            return false;
        }
        return count(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, roleCode)) > 0;
    }
}