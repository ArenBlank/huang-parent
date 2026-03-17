package com.huang.web.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huang.model.entity.UserProfile;

public interface UserProfileService extends IService<UserProfile> {
    UserProfile getByUserId(Long userId);
}
