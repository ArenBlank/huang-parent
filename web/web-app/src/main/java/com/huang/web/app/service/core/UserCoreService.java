package com.huang.web.app.service.core;

import com.huang.model.entity.User;
import com.huang.web.app.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserCoreService {

    private final UserService userService;

    public UserCoreService(UserService userService) {
        this.userService = userService;
    }

    public User getByUsername(String username) {
        return userService.getByUsername(username);
    }

    public User getByPhone(String phone) {
        return userService.getByPhone(phone);
    }

    public boolean existsByUsername(String username) {
        return userService.existsByUsername(username);
    }

    public boolean existsByPhone(String phone) {
        return userService.existsByPhone(phone);
    }

    public boolean existsByEmail(String email) {
        return userService.existsByEmail(email);
    }

    public boolean save(User user) {
        return userService.save(user);
    }

    public boolean updateById(User user) {
        return userService.updateById(user);
    }
}

