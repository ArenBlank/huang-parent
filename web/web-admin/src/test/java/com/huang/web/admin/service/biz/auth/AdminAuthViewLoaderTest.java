package com.huang.web.admin.service.biz.auth;

import com.huang.model.entity.Role;
import com.huang.model.entity.User;
import com.huang.web.admin.custom.config.AdminPermissionProperties;
import com.huang.web.admin.mapper.AdminAuthViewQueryMapper;
import com.huang.web.admin.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthViewLoaderTest {

    @Mock
    private UserService userService;

    @Mock
    private AdminAuthViewQueryMapper adminAuthViewQueryMapper;

    @Mock
    private AdminPermissionProperties adminPermissionProperties;

    @InjectMocks
    private AdminAuthViewLoader adminAuthViewLoader;

    @Test
    void load_shouldReturnNullWhenUserNotFound() {
        when(userService.getById(1L)).thenReturn(null);

        AdminAuthView view = adminAuthViewLoader.load(1L);

        assertNull(view);
    }

    @Test
    void load_shouldAssembleViewAndMergeDbAndConfigPermissionsAndScopes() {
        User user = new User();
        user.setId(1L);
        user.setUsername("root_admin");
        user.setUserType("admin");
        user.setStatus(1);
        user.setTokenVersion(-3);

        Role adminRole = new Role();
        adminRole.setId(10L);
        adminRole.setRoleCode("ADMIN");

        Role opsRole = new Role();
        opsRole.setId(20L);
        opsRole.setRoleCode("OPS_ADMIN");

        when(userService.getById(1L)).thenReturn(user);
        when(adminAuthViewQueryMapper.selectActiveRolesByUserId(1L)).thenReturn(List.of(adminRole, opsRole));
        when(adminAuthViewQueryMapper.selectPermissionCodesByRoleIds(Set.of(10L, 20L)))
                .thenReturn(List.of("dashboard:read", "course:update", "dashboard:read"));
        when(adminAuthViewQueryMapper.selectCategoryIdsByRoleIds(Set.of(10L, 20L)))
                .thenReturn(List.of(1L, 2L, 1L));
        when(adminPermissionProperties.isAdminAll()).thenReturn(true);
        when(adminPermissionProperties.permissionsFor("ADMIN")).thenReturn(Set.of("*"));
        when(adminPermissionProperties.permissionsFor("OPS_ADMIN")).thenReturn(Set.of("video:bind"));
        when(adminPermissionProperties.courseCategoriesFor("ADMIN")).thenReturn(Set.of());
        when(adminPermissionProperties.courseCategoriesFor("OPS_ADMIN")).thenReturn(Set.of(9L));

        AdminAuthView view = adminAuthViewLoader.load(1L);

        assertNotNull(view);
        assertEquals(1L, view.userId());
        assertEquals("root_admin", view.username());
        assertEquals("admin", view.userType());
        assertEquals(1, view.status());
        assertEquals(0, view.tokenVersion());
        assertEquals(new LinkedHashSet<>(Set.of(10L, 20L)), view.roleIds());
        assertEquals(new LinkedHashSet<>(Set.of("ADMIN", "OPS_ADMIN")), view.roleCodes());
        assertTrue(view.permissionCodes().contains("dashboard:read"));
        assertTrue(view.permissionCodes().contains("course:update"));
        assertTrue(view.permissionCodes().contains("video:bind"));
        assertTrue(view.permissionCodes().contains("*"));
        assertEquals(new LinkedHashSet<>(Set.of(1L, 2L, 9L)), view.allowedCourseCategoryIds());
        assertTrue(view.adminAll());
    }

    @Test
    void load_shouldKeepAdminAllFalseWhenAdminRoleMissing() {
        User user = new User();
        user.setId(2L);
        user.setUsername("ops");
        user.setUserType("admin");
        user.setStatus(1);
        user.setTokenVersion(7);

        Role opsRole = new Role();
        opsRole.setId(20L);
        opsRole.setRoleCode("OPS_ADMIN");

        when(userService.getById(2L)).thenReturn(user);
        when(adminAuthViewQueryMapper.selectActiveRolesByUserId(2L)).thenReturn(List.of(opsRole));
        when(adminAuthViewQueryMapper.selectPermissionCodesByRoleIds(Set.of(20L))).thenReturn(List.of());
        when(adminAuthViewQueryMapper.selectCategoryIdsByRoleIds(Set.of(20L))).thenReturn(List.of());
        when(adminPermissionProperties.isAdminAll()).thenReturn(true);
        when(adminPermissionProperties.permissionsFor("OPS_ADMIN")).thenReturn(Set.of());
        when(adminPermissionProperties.courseCategoriesFor("OPS_ADMIN")).thenReturn(Set.of());

        AdminAuthView view = adminAuthViewLoader.load(2L);

        assertNotNull(view);
        assertEquals(7, view.tokenVersion());
        assertFalse(view.adminAll());
    }
}
