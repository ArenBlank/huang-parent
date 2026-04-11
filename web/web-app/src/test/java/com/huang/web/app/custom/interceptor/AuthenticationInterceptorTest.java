package com.huang.web.app.custom.interceptor;

import com.huang.common.exception.HuangException;
import com.huang.common.login.LoginUserHolder;
import com.huang.common.result.ResultCodeEnum;
import com.huang.common.utils.JwtUtil;
import com.huang.web.app.service.biz.auth.AppAuthCacheService;
import com.huang.web.app.service.biz.auth.AppAuthSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationInterceptorTest {

    @Mock
    private AppAuthCacheService appAuthCacheService;

    @AfterEach
    void tearDown() {
        LoginUserHolder.clear();
    }

    @Test
    void preHandle_shouldAuthenticateWithCachedSnapshot() throws Exception {
        AuthenticationInterceptor interceptor = new AuthenticationInterceptor(appAuthCacheService);
        when(appAuthCacheService.getOrLoad(21L)).thenReturn(new AppAuthSnapshot(1, 2));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/profile/info");
        request.addHeader("Authorization", "Bearer " + JwtUtil.generateAppAccessToken(21L, "member", 2));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        assertNotNull(LoginUserHolder.getLoginUser());
        assertEquals(21L, LoginUserHolder.getLoginUser().getUserId());

        interceptor.afterCompletion(request, response, new Object(), null);

        assertEquals(null, LoginUserHolder.getLoginUser());
    }

    @Test
    void preHandle_shouldRejectWhenTokenVersionDoesNotMatch() {
        AuthenticationInterceptor interceptor = new AuthenticationInterceptor(appAuthCacheService);
        when(appAuthCacheService.getOrLoad(22L)).thenReturn(new AppAuthSnapshot(1, 9));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/profile/info");
        request.addHeader("Authorization", "Bearer " + JwtUtil.generateAppAccessToken(22L, "member", 2));

        HuangException exception = assertThrows(
                HuangException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );

        assertEquals(ResultCodeEnum.TOKEN_INVALID.getCode(), exception.getCode());
    }
}
