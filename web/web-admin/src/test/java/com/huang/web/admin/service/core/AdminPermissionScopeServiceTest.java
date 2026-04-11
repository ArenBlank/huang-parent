package com.huang.web.admin.service.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.exception.HuangException;
import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.common.result.ResultCodeEnum;
import com.huang.model.entity.Role;
import com.huang.model.entity.RoleCourseCategoryScope;
import com.huang.web.admin.custom.config.AdminPermissionProperties;
import com.huang.web.admin.mapper.RoleCourseCategoryScopeMapper;
import com.huang.web.admin.service.RoleService;
import com.huang.web.admin.service.biz.AdminOperationLogBizService;
import com.huang.web.admin.service.biz.auth.AdminAuthView;
import com.huang.web.admin.service.biz.auth.AdminAuthViewHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPermissionScopeServiceTest {

    @Mock
    private AdminPermissionProperties adminPermissionProperties;

    @Mock
    private AdminOperationLogBizService adminOperationLogBizService;

    @Mock
    private RoleService roleService;

    @Mock
    private RoleCourseCategoryScopeMapper roleCourseCategoryScopeMapper;

    @AfterEach
    void tearDown() {
        LoginUserHolder.clear();
        AdminAuthViewHolder.clear();
    }

    @Test
    void assertCourseCategoryAccess_shouldUseHolderFirstWithoutTouchingDb() {
        AdminPermissionScopeService service = new AdminPermissionScopeService(
                adminPermissionProperties,
                adminOperationLogBizService,
                roleService,
                roleCourseCategoryScopeMapper
        );
        LoginUserHolder.setLoginUser(new LoginUser(1L, "admin", Set.of("OPS_ADMIN")));
        AdminAuthViewHolder.set(new AdminAuthView(
                1L,
                "admin",
                "admin",
                1,
                0,
                new LinkedHashSet<>(Set.of(10L)),
                new LinkedHashSet<>(Set.of("OPS_ADMIN")),
                new LinkedHashSet<>(Set.of("course:update")),
                new LinkedHashSet<>(Set.of(9L, 10L)),
                false
        ));

        service.assertCourseCategoryAccess(9L, "course_update", "from_holder");

        verifyNoInteractions(roleService, roleCourseCategoryScopeMapper);
        verify(adminOperationLogBizService, never()).record(any(), any(), any(), anyBoolean());
    }

    @Test
    void assertCourseCategoryAccess_shouldDenyWhenHolderScopeDoesNotContainCategory() {
        AdminPermissionScopeService service = new AdminPermissionScopeService(
                adminPermissionProperties,
                adminOperationLogBizService,
                roleService,
                roleCourseCategoryScopeMapper
        );
        LoginUserHolder.setLoginUser(new LoginUser(2L, "ops", Set.of("OPS_ADMIN")));
        AdminAuthViewHolder.set(new AdminAuthView(
                2L,
                "ops",
                "admin",
                1,
                0,
                new LinkedHashSet<>(Set.of(20L)),
                new LinkedHashSet<>(Set.of("OPS_ADMIN")),
                new LinkedHashSet<>(Set.of("course:update")),
                new LinkedHashSet<>(Set.of(9L)),
                false
        ));

        HuangException exception = assertThrows(
                HuangException.class,
                () -> service.assertCourseCategoryAccess(88L, "course_update", "holder_deny")
        );

        assertEquals(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN.getCode(), exception.getCode());
        verify(adminOperationLogBizService).record("authz", "course_update", "categoryId=88, holder_deny", false);
        verifyNoInteractions(roleService, roleCourseCategoryScopeMapper);
    }

    @Test
    void assertCourseCategoryAccess_shouldFallbackToLegacyDbPathWhenHolderMissing() {
        AdminPermissionScopeService service = new AdminPermissionScopeService(
                adminPermissionProperties,
                adminOperationLogBizService,
                roleService,
                roleCourseCategoryScopeMapper
        );
        LoginUserHolder.setLoginUser(new LoginUser(3L, "legacy", Set.of("OPS_ADMIN")));
        when(adminPermissionProperties.isAdminAll()).thenReturn(false);

        Role role = new Role();
        role.setId(30L);
        role.setRoleCode("OPS_ADMIN");
        when(roleService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));

        RoleCourseCategoryScope scope = new RoleCourseCategoryScope();
        scope.setRoleId(30L);
        scope.setCategoryId(7L);
        when(roleCourseCategoryScopeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(scope));

        service.assertCourseCategoryAccess(7L, "course_update", "legacy_fallback");

        verify(roleService).list(any(LambdaQueryWrapper.class));
        verify(roleCourseCategoryScopeMapper).selectList(any(LambdaQueryWrapper.class));
    }
}
