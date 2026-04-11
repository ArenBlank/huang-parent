package com.huang.web.admin.service.biz.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AdminAuthCacheEvictionListenerTest {

    @Mock
    private AdminAuthCacheService adminAuthCacheService;

    @InjectMocks
    private AdminAuthCacheEvictionListener adminAuthCacheEvictionListener;

    @Test
    void onAdminUserAuthChanged_shouldEvictUser() {
        adminAuthCacheEvictionListener.onAdminUserAuthChanged(new AdminUserAuthChangedEvent(1L, "status_changed"));

        verify(adminAuthCacheService).evictUser(1L);
    }

    @Test
    void onAdminUserRolesChanged_shouldEvictUser() {
        adminAuthCacheEvictionListener.onAdminUserRolesChanged(new AdminUserRolesChangedEvent(2L, "assign_roles"));

        verify(adminAuthCacheService).evictUser(2L);
    }

    @Test
    void onAdminRoleAclChanged_shouldEvictUsersByRole() {
        adminAuthCacheEvictionListener.onAdminRoleAclChanged(new AdminRoleAclChangedEvent(10L, "role_permission_replace"));

        verify(adminAuthCacheService).evictUsersByRole(10L);
    }

    @Test
    void listener_shouldIgnoreNullIdentifiers() {
        adminAuthCacheEvictionListener.onAdminUserAuthChanged(new AdminUserAuthChangedEvent(null, "x"));
        adminAuthCacheEvictionListener.onAdminUserRolesChanged(new AdminUserRolesChangedEvent(null, "x"));
        adminAuthCacheEvictionListener.onAdminRoleAclChanged(new AdminRoleAclChangedEvent(null, "x"));

        verifyNoInteractions(adminAuthCacheService);
    }
}
