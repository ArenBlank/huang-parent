package com.huang.web.admin.service.biz.auth;

import com.huang.model.entity.User;
import com.huang.web.admin.dto.user.UserStatusUpdateDTO;
import com.huang.web.admin.service.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserAuthBizService {

    private final UserService userService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final StringRedisTemplate stringRedisTemplate;

    public AdminUserAuthBizService(UserService userService,
                                   ApplicationEventPublisher applicationEventPublisher,
                                   StringRedisTemplate stringRedisTemplate) {
        this.userService = userService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(UserStatusUpdateDTO dto) {
        if (dto == null || dto.getUserId() == null) {
            return false;
        }
        User user = userService.getById(dto.getUserId());
        if (user == null) {
            return false;
        }
        user.setStatus(dto.getStatus());
        boolean updated = userService.updateById(user);
        if (updated) {
            stringRedisTemplate.delete("app:auth:user:" + dto.getUserId());
            applicationEventPublisher.publishEvent(new AdminUserAuthChangedEvent(dto.getUserId(), "status_changed"));
        }
        return updated;
    }
}
