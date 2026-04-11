package com.huang.web.admin.service.biz.auth;

import com.huang.model.entity.Role;
import com.huang.web.admin.dto.role.RoleStatusUpdateDTO;
import com.huang.web.admin.service.RoleService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminRoleAclBizService {

    private final RoleService roleService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public AdminRoleAclBizService(RoleService roleService,
                                  ApplicationEventPublisher applicationEventPublisher) {
        this.roleService = roleService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(RoleStatusUpdateDTO dto) {
        if (dto == null || dto.getRoleId() == null) {
            return false;
        }
        Role role = roleService.getById(dto.getRoleId());
        if (role == null) {
            return false;
        }
        role.setStatus(dto.getStatus());
        boolean updated = roleService.updateById(role);
        if (updated) {
            applicationEventPublisher.publishEvent(new AdminRoleAclChangedEvent(dto.getRoleId(), "role_status_changed"));
        }
        return updated;
    }
}
