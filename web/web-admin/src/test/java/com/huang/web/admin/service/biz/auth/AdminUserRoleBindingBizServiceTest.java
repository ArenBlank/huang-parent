package com.huang.web.admin.service.biz.auth;

import com.huang.model.entity.User;
import com.huang.model.entity.UserRole;
import com.huang.web.admin.dto.user.UserRoleAssignDTO;
import com.huang.web.admin.dto.userrole.BatchRoleAssignDTO;
import com.huang.web.admin.service.UserRoleService;
import com.huang.web.admin.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserRoleBindingBizServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRoleService userRoleService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private AdminUserRoleBindingBizService adminUserRoleBindingBizService;

    @Test
    void assignRoles_shouldPublishEventAfterReplacement() {
        UserRoleAssignDTO dto = new UserRoleAssignDTO();
        dto.setUserId(1L);
        dto.setRoleIds(List.of(10L, 20L));
        User user = new User();
        user.setId(1L);

        when(userService.getById(1L)).thenReturn(user);

        boolean ok = adminUserRoleBindingBizService.assignRoles(dto);

        assertTrue(ok);
        verify(userRoleService).remove(any());
        verify(userRoleService, atLeastOnce()).save(any(UserRole.class));
        verify(applicationEventPublisher).publishEvent(new AdminUserRolesChangedEvent(1L, "assign_roles"));
    }

    @Test
    void batchAssign_shouldRejectUnsupportedOperation() {
        BatchRoleAssignDTO dto = new BatchRoleAssignDTO();
        dto.setUserIds(List.of(1L));
        dto.setRoleIds(List.of(10L));
        dto.setOperation("invalid");

        boolean ok = adminUserRoleBindingBizService.batchAssign(dto);

        assertFalse(ok);
        verifyNoInteractions(userService);
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void removeRole_shouldPublishEventWhenLinkDeleted() {
        when(userRoleService.remove(any())).thenReturn(true);

        boolean ok = adminUserRoleBindingBizService.removeRole(2L, 8L);

        assertTrue(ok);
        verify(applicationEventPublisher).publishEvent(new AdminUserRolesChangedEvent(2L, "remove_role"));
    }
}
