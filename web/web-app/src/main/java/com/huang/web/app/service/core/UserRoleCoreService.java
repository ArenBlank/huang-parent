package com.huang.web.app.service.core;

import com.huang.model.entity.UserRole;
import com.huang.web.app.service.UserRoleService;
import org.springframework.stereotype.Service;

@Service
public class UserRoleCoreService {

    private final UserRoleService userRoleService;

    public UserRoleCoreService(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    public boolean save(UserRole userRole) {
        return userRoleService.save(userRole);
    }
}

