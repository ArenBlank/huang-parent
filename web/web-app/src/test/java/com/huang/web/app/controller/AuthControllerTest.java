package com.huang.web.app.controller;

import com.huang.common.result.Result;
import com.huang.model.entity.User;
import com.huang.web.app.dto.auth.ForgetPasswordDTO;
import com.huang.web.app.service.biz.auth.AppAuthCacheService;
import com.huang.web.app.service.core.RoleCoreService;
import com.huang.web.app.service.core.UserCoreService;
import com.huang.web.app.service.core.UserRoleCoreService;
import com.huang.common.utils.SmsCodeUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private SmsCodeUtil smsCodeUtil;

    @Mock
    private UserCoreService userCoreService;

    @Mock
    private RoleCoreService roleCoreService;

    @Mock
    private UserRoleCoreService userRoleCoreService;

    @Mock
    private AppAuthCacheService appAuthCacheService;

    @Test
    void forgetPassword_shouldIncreaseTokenVersionAndEvictAuthCache() {
        AuthController controller = new AuthController(
                smsCodeUtil,
                userCoreService,
                roleCoreService,
                userRoleCoreService,
                appAuthCacheService
        );
        User user = new User();
        user.setId(8L);
        user.setPhone("13800000000");
        user.setTokenVersion(0);
        when(smsCodeUtil.verifySmsCode("13800000000", "123456", "reset_password")).thenReturn(true);
        when(userCoreService.getByPhone("13800000000")).thenReturn(user);
        when(userCoreService.updateById(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(true);

        ForgetPasswordDTO dto = new ForgetPasswordDTO();
        dto.setPhone("13800000000");
        dto.setSmsCode("123456");
        dto.setNewPassword("new-pass");
        dto.setConfirmPassword("new-pass");

        Result<String> result = controller.forgetPassword(dto);

        assertEquals(200, result.getCode());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userCoreService).updateById(captor.capture());
        assertEquals(1, captor.getValue().getTokenVersion());
        verify(appAuthCacheService).evict(8L);
    }
}
