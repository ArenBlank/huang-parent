package com.huang.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.model.entity.Role;
import com.huang.web.app.dto.role.RoleQueryDTO;
import com.huang.web.app.mapper.RoleMapper;
import com.huang.web.app.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 角色服务实现类
 * @author system
 * @since 2025-01-24
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Override
    public Role getByRoleCode(String roleCode) {
        if (roleCode == null || roleCode.trim().isEmpty()) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, roleCode)
                .last("LIMIT 1"));
    }

    @Override
    public boolean existsByRoleCode(String roleCode) {
        if (roleCode == null || roleCode.trim().isEmpty()) {
            return false;
        }
        return count(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, roleCode)) > 0;
    }

    @Override
    public Page<Role> getPageList(RoleQueryDTO queryDTO) {
        // 构建分页对象
        Page<Role> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        
        // 构建查询条件
        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
        
        // 角色名称模糊查询
        if (StringUtils.hasText(queryDTO.getRoleName())) {
            queryWrapper.like(Role::getRoleName, queryDTO.getRoleName());
        }
        
        // 角色编码模糊查询
        if (StringUtils.hasText(queryDTO.getRoleCode())) {
            queryWrapper.like(Role::getRoleCode, queryDTO.getRoleCode());
        }
        
        // 状态筛选
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq(Role::getStatus, queryDTO.getStatus());
        }
        
        // 时间范围筛选
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (StringUtils.hasText(queryDTO.getStartTime())) {
            queryWrapper.ge(Role::getCreateTime, LocalDateTime.parse(queryDTO.getStartTime(), formatter));
        }
        if (StringUtils.hasText(queryDTO.getEndTime())) {
            queryWrapper.le(Role::getCreateTime, LocalDateTime.parse(queryDTO.getEndTime(), formatter));
        }
        
        // 排序
        if ("asc".equalsIgnoreCase(queryDTO.getSortOrder())) {
            queryWrapper.orderByAsc(Role::getCreateTime);
        } else {
            queryWrapper.orderByDesc(Role::getCreateTime);
        }
        
        return page(page, queryWrapper);
    }
}
