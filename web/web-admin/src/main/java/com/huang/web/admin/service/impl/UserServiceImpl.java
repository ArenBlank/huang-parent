package com.huang.web.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.model.entity.User;
import com.huang.web.admin.mapper.UserMapper;
import com.huang.web.admin.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 * @author system
 * @since 2025-01-24
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User getByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
    }

    @Override
    public User getByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .last("LIMIT 1"));
    }

    @Override
    public User getByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .last("LIMIT 1"));
    }

    @Override
    public boolean existsByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)) > 0;
    }

    @Override
    public boolean existsByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return count(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return count(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)) > 0;
    }
}