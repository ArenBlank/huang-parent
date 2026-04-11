package com.huang.web.admin.custom.interceptor;

import com.huang.common.exception.HuangException;
import com.huang.common.login.LoginUserHolder;
import com.huang.common.result.ResultCodeEnum;
import com.huang.common.utils.JwtUtil;
import com.huang.web.admin.custom.annotation.RequireAdminPermission;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.custom.config.AdminPermissionProperties;
import com.huang.web.admin.service.biz.AdminOperationLogBizService;
import com.huang.web.admin.service.biz.auth.AdminAuthCacheService;
import com.huang.web.admin.service.biz.auth.AdminAuthView;
import com.huang.web.admin.service.biz.auth.AdminAuthViewHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationInterceptorTest {

    @Mock
    private AdminAuthCacheService adminAuthCacheService;

    @Mock
    private AdminPermissionProperties adminPermissionProperties;

    @Mock
    private AdminOperationLogBizService adminOperationLogBizService;

    @AfterEach
    void tearDown() {
        LoginUserHolder.clear();
        AdminAuthViewHolder.clear();
    }

    @Test
    void preHandle_shouldAuthenticateWithCachedAdminAuthView() throws Exception {
        AuthenticationInterceptor interceptor = new AuthenticationInterceptor(
                adminAuthCacheService,
                adminPermissionProperties,
                adminOperationLogBizService
        );
        AdminAuthView authView = new AdminAuthView(
                1L,
                "root_admin",
                "admin",
                1,
                3,
                new LinkedHashSet<>(Set.of(10L)),
                new LinkedHashSet<>(Set.of("OPS_ADMIN")),
                new LinkedHashSet<>(Set.of("course:update")),
                new LinkedHashSet<>(Set.of(1L, 2L)),
                false
        );
        when(adminAuthCacheService.getOrLoad(1L)).thenReturn(authView);
        when(adminPermissionProperties.isAdminAll()).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/admin/course/1");
        request.addHeader("Authorization", "Bearer " + JwtUtil.generateAdminAccessToken(1L, "root_admin", 3));
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handlerMethod = handlerMethod("protectedEndpoint");

        boolean allowed = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(allowed);
        assertNotNull(LoginUserHolder.getLoginUser());
        assertEquals(1L, LoginUserHolder.getLoginUser().getUserId());
        assertSame(authView, AdminAuthViewHolder.get());

        interceptor.afterCompletion(request, response, handlerMethod, null);

        assertEquals(null, LoginUserHolder.getLoginUser());
        assertEquals(null, AdminAuthViewHolder.get());
        verify(adminOperationLogBizService, never()).record(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void preHandle_shouldRejectWhenCachedPermissionsDoNotMatch() throws Exception {
        AuthenticationInterceptor interceptor = new AuthenticationInterceptor(
                adminAuthCacheService,
                adminPermissionProperties,
                adminOperationLogBizService
        );
        AdminAuthView authView = new AdminAuthView(
                2L,
                "ops_admin",
                "admin",
                1,
                1,
                new LinkedHashSet<>(Set.of(20L)),
                new LinkedHashSet<>(Set.of("OPS_ADMIN")),
                new LinkedHashSet<>(Set.of("dashboard:read")),
                new LinkedHashSet<>(Set.of(1L)),
                false
        );
        when(adminAuthCacheService.getOrLoad(2L)).thenReturn(authView);
        when(adminPermissionProperties.isAdminAll()).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/admin/course/1");
        request.addHeader("Authorization", "Bearer " + JwtUtil.generateAdminAccessToken(2L, "ops_admin", 1));
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handlerMethod = handlerMethod("protectedEndpoint");

        HuangException exception = assertThrows(
                HuangException.class,
                () -> interceptor.preHandle(request, response, handlerMethod)
        );

        assertEquals(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN.getCode(), exception.getCode());
        verify(adminOperationLogBizService).record("authz", "perm_deny", "method=PUT, uri=/admin/course/1, required=[course:update]", false);
    }

    @Test
    void preHandle_shouldRejectWhenRoleDoesNotMatch() throws Exception {
        AuthenticationInterceptor interceptor = new AuthenticationInterceptor(
                adminAuthCacheService,
                adminPermissionProperties,
                adminOperationLogBizService
        );
        AdminAuthView authView = new AdminAuthView(
                3L,
                "viewer",
                "admin",
                1,
                0,
                new LinkedHashSet<>(Set.of(30L)),
                new LinkedHashSet<>(Set.of("VIEWER")),
                new LinkedHashSet<>(Set.of("course:update")),
                new LinkedHashSet<>(Set.of(1L)),
                false
        );
        when(adminAuthCacheService.getOrLoad(3L)).thenReturn(authView);

        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/admin/course/1");
        request.addHeader("Authorization", "Bearer " + JwtUtil.generateAdminAccessToken(3L, "viewer", 0));
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handlerMethod = handlerMethod("roleProtectedEndpoint");

        HuangException exception = assertThrows(
                HuangException.class,
                () -> interceptor.preHandle(request, response, handlerMethod)
        );

        assertEquals(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN.getCode(), exception.getCode());
        verify(adminOperationLogBizService).record("authz", "role_deny", "method=PUT, uri=/admin/course/1, required=[OPS_ADMIN]", false);
    }

    private HandlerMethod handlerMethod(String methodName) throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod(methodName);
        return new HandlerMethod(new TestController(), method);
    }

    private static class TestController {

        @RequireAdminPermission("course:update")
        public void protectedEndpoint() {
        }

        @RequireAdminRole("OPS_ADMIN")
        public void roleProtectedEndpoint() {
        }
    }
}
