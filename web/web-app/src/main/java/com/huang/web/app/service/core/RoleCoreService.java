package com.huang.web.app.service.core;

import com.huang.model.entity.Role;
import com.huang.web.app.service.RoleService;
import org.springframework.stereotype.Service;

@Service
public class RoleCoreService {

    private final RoleService roleService;

    public RoleCoreService(RoleService roleService) {
        this.roleService = roleService;
    }

    public Role getByRoleCode(String roleCode) {
        return roleService.getByRoleCode(roleCode);
    }
}

