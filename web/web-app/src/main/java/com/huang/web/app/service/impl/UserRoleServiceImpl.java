package com.huang.web.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.model.entity.UserRole;
import com.huang.web.app.mapper.UserRoleMapper;
import com.huang.web.app.service.UserRoleService;
import org.springframework.stereotype.Service;

/**
 * UserRole服务实现类
 * @author system
 * @since 2025-01-24
 */
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {

}
