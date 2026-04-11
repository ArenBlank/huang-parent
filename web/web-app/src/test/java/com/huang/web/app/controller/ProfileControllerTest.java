package com.huang.web.app.controller;

import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.common.minio.MinioProperties;
import com.huang.common.result.Result;
import com.huang.common.utils.PasswordUtil;
import com.huang.model.entity.User;
import com.huang.web.app.dto.profile.PasswordUpdateDTO;
import com.huang.web.app.service.biz.auth.AppAuthCacheService;
import com.huang.web.app.service.core.UserCoreService;
import com.huang.web.app.service.core.UserProfileCoreService;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private UserCoreService userCoreService;

    @Mock
    private UserProfileCoreService userProfileCoreService;

    @Mock
    private AppAuthCacheService appAuthCacheService;

    @Mock
    private ObjectProvider<MinioClient> minioClientProvider;

    @AfterEach
    void tearDown() {
        LoginUserHolder.clear();
    }

    @Test
    void updatePassword_shouldIncreaseTokenVersionAndEvictAuthCache() {
        when(minioClientProvider.getIfAvailable()).thenReturn(null);
        ProfileController controller = new ProfileController(
                userCoreService,
                userProfileCoreService,
                appAuthCacheService,
                minioClientProvider,
                new MinioProperties()
        );
        LoginUserHolder.setLoginUser(new LoginUser(1L, "member"));
        User user = new User();
        user.setId(1L);
        user.setPassword(PasswordUtil.encode("old-password"));
        user.setTokenVersion(2);
        when(userCoreService.getById(1L)).thenReturn(user);
        when(userCoreService.updateById(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(true);

        PasswordUpdateDTO dto = new PasswordUpdateDTO();
        dto.setOldPassword("old-password");
        dto.setNewPassword("new-password");
        dto.setConfirmPassword("new-password");

        Result<String> result = controller.updatePassword(dto);

        assertEquals(200, result.getCode());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userCoreService).updateById(captor.capture());
        assertEquals(3, captor.getValue().getTokenVersion());
        assertTrue(PasswordUtil.matches("new-password", captor.getValue().getPassword()));
        verify(appAuthCacheService).evict(1L);
    }
}
