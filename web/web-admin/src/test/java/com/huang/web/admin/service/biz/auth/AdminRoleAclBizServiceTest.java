package com.huang.web.admin.service.biz.auth;

import com.huang.model.entity.Role;
import com.huang.web.admin.dto.role.RoleStatusUpdateDTO;
import com.huang.web.admin.service.RoleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRoleAclBizServiceTest {

    @Mock
    private RoleService roleService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private AdminRoleAclBizService adminRoleAclBizService;

    @Test
    void updateStatus_shouldPublishRoleAclEventWhenSuccessful() {
        Role role = new Role();
        role.setId(3L);
        role.setStatus(1);
        RoleStatusUpdateDTO dto = new RoleStatusUpdateDTO();
        dto.setRoleId(3L);
        dto.setStatus(0);

        when(roleService.getById(3L)).thenReturn(role);
        when(roleService.updateById(role)).thenReturn(true);

        boolean ok = adminRoleAclBizService.updateStatus(dto);

        assertTrue(ok);
        verify(applicationEventPublisher).publishEvent(new AdminRoleAclChangedEvent(3L, "role_status_changed"));
    }
}
