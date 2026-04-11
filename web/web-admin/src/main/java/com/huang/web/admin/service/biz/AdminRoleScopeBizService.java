package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.model.entity.Role;
import com.huang.model.entity.RoleCourseCategoryScope;
import com.huang.web.admin.dto.role.RoleCourseCategoryScopeItemDTO;
import com.huang.web.admin.mapper.RoleCourseCategoryScopeMapper;
import com.huang.web.admin.service.RoleService;
import com.huang.web.admin.service.biz.auth.AdminRoleAclChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminRoleScopeBizService {

    private final RoleService roleService;
    private final RoleCourseCategoryScopeMapper roleCourseCategoryScopeMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public AdminRoleScopeBizService(RoleService roleService,
                                    RoleCourseCategoryScopeMapper roleCourseCategoryScopeMapper,
                                    ApplicationEventPublisher applicationEventPublisher) {
        this.roleService = roleService;
        this.roleCourseCategoryScopeMapper = roleCourseCategoryScopeMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public List<Long> listCourseCategoryScope(Long roleId) {
        return roleCourseCategoryScopeMapper.selectList(new LambdaQueryWrapper<RoleCourseCategoryScope>()
                        .eq(RoleCourseCategoryScope::getRoleId, roleId))
                .stream()
                .map(RoleCourseCategoryScope::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateCourseCategoryScope(Long roleId, List<Long> categoryIds) {
        Role role = roleService.getById(roleId);
        if (role == null) {
            return false;
        }
        roleCourseCategoryScopeMapper.deleteByRoleIdPhysical(roleId);
        if (categoryIds != null) {
            for (Long categoryId : categoryIds) {
                if (categoryId == null) {
                    continue;
                }
                RoleCourseCategoryScope scope = new RoleCourseCategoryScope();
                scope.setRoleId(roleId);
                scope.setCategoryId(categoryId);
                roleCourseCategoryScopeMapper.insert(scope);
            }
        }
        applicationEventPublisher.publishEvent(new AdminRoleAclChangedEvent(roleId, "role_scope_update"));
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateCourseCategoryScopeBatch(List<RoleCourseCategoryScopeItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return true;
        }
        for (RoleCourseCategoryScopeItemDTO item : items) {
            if (item == null || item.getRoleId() == null) {
                return false;
            }
            Role role = roleService.getById(item.getRoleId());
            if (role == null) {
                return false;
            }
        }
        for (RoleCourseCategoryScopeItemDTO item : items) {
            updateCourseCategoryScope(item.getRoleId(), item.getCategoryIds());
        }
        return true;
    }
}
