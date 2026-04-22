package com.huang.web.admin.controller;

import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.service.CaptchaService;
import com.huang.common.result.Result;
import com.huang.common.utils.PasswordUtil;
import com.huang.model.entity.User;
import com.huang.web.admin.dto.auth.AdminLoginDTO;
import com.huang.web.admin.service.UserService;
import com.huang.web.admin.service.core.AdminRoleCoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AdminRoleCoreService adminRoleCoreService;

    @Mock
    private CaptchaService captchaService;

    @Test
    void login_shouldRejectWhenCaptchaIsInvalid() {
        AdminAuthController controller = new AdminAuthController(userService, adminRoleCoreService, captchaService);
        ResponseModel responseModel = mock(ResponseModel.class);
        when(responseModel.isSuccess()).thenReturn(false);
        when(captchaService.verification(any())).thenReturn(responseModel);

        AdminLoginDTO dto = new AdminLoginDTO();
        dto.setAccount("root_admin");
        dto.setPassword("root");
        dto.setCaptchaVerification("bad-token");

        Result<?> result = controller.login(dto);

        assertEquals(201, result.getCode());
        assertEquals("Slider captcha is invalid or expired", result.getMessage());
        verify(userService, never()).getOne(any());
    }

    @Test
    void login_shouldSucceedWhenCaptchaAndAdminAccountAreValid() {
        AdminAuthController controller = new AdminAuthController(userService, adminRoleCoreService, captchaService);
        ResponseModel responseModel = mock(ResponseModel.class);
        when(responseModel.isSuccess()).thenReturn(true);
        when(captchaService.verification(any())).thenReturn(responseModel);

        User user = new User();
        user.setId(99L);
        user.setUsername("root_admin");
        user.setPassword(PasswordUtil.encode("root"));
        user.setStatus(1);
        user.setUserType("admin");
        user.setTokenVersion(0);
        when(userService.getOne(any())).thenReturn(user);
        when(adminRoleCoreService.getRoleCodes(99L)).thenReturn(Set.of("ROOT_ADMIN"));

        AdminLoginDTO dto = new AdminLoginDTO();
        dto.setAccount("root_admin");
        dto.setPassword("root");
        dto.setCaptchaVerification("good-token");

        Result<?> result = controller.login(dto);

        assertEquals(200, result.getCode());
        assertInstanceOf(Map.class, result.getData());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals("root_admin", data.get("username"));
        assertEquals(Set.of("ROOT_ADMIN"), data.get("roleCodes"));
        assertNotNull(data.get("accessToken"));
        assertNotNull(data.get("refreshToken"));
    }
}
