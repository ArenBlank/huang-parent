package com.huang.web.admin.service.biz.auth;

import com.huang.model.entity.User;
import com.huang.web.admin.dto.user.UserStatusUpdateDTO;
import com.huang.web.admin.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserAuthBizServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private AdminUserAuthBizService adminUserAuthBizService;

    @Test
    void updateStatus_shouldPublishEventWhenUpdateSucceeds() {
        User user = new User();
        user.setId(1L);
        user.setStatus(1);
        UserStatusUpdateDTO dto = new UserStatusUpdateDTO();
        dto.setUserId(1L);
        dto.setStatus(0);

        when(userService.getById(1L)).thenReturn(user);
        when(userService.updateById(user)).thenReturn(true);

        boolean ok = adminUserAuthBizService.updateStatus(dto);

        assertTrue(ok);
        verify(stringRedisTemplate).delete("app:auth:user:1");
        verify(applicationEventPublisher).publishEvent(new AdminUserAuthChangedEvent(1L, "status_changed"));
    }

    @Test
    void updateStatus_shouldReturnFalseWhenUserMissing() {
        UserStatusUpdateDTO dto = new UserStatusUpdateDTO();
        dto.setUserId(9L);
        dto.setStatus(0);
        when(userService.getById(9L)).thenReturn(null);

        boolean ok = adminUserAuthBizService.updateStatus(dto);

        assertFalse(ok);
        verifyNoInteractions(applicationEventPublisher);
    }
}
