package com.huang.web.admin.service.biz.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.model.entity.User;
import com.huang.model.entity.UserRole;
import com.huang.web.admin.dto.user.UserRoleAssignDTO;
import com.huang.web.admin.dto.userrole.BatchRoleAssignDTO;
import com.huang.web.admin.service.UserRoleService;
import com.huang.web.admin.service.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminUserRoleBindingBizService {

    private final UserService userService;
    private final UserRoleService userRoleService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public AdminUserRoleBindingBizService(UserService userService,
                                          UserRoleService userRoleService,
                                          ApplicationEventPublisher applicationEventPublisher) {
        this.userService = userService;
        this.userRoleService = userRoleService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean assignRoles(UserRoleAssignDTO dto) {
        if (dto == null || dto.getUserId() == null || CollectionUtils.isEmpty(dto.getRoleIds())) {
            return false;
        }
        User user = userService.getById(dto.getUserId());
        if (user == null) {
            return false;
        }
        Set<Long> roleIds = uniqueIds(dto.getRoleIds());
        if (roleIds.isEmpty()) {
            return false;
        }

        userRoleService.remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, dto.getUserId()));
        for (Long roleId : roleIds) {
            UserRole userRole = new UserRole();
            userRole.setUserId(dto.getUserId());
            userRole.setRoleId(roleId);
            userRoleService.save(userRole);
        }
        applicationEventPublisher.publishEvent(new AdminUserRolesChangedEvent(dto.getUserId(), "assign_roles"));
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean batchAssign(BatchRoleAssignDTO dto) {
        if (dto == null || CollectionUtils.isEmpty(dto.getUserIds()) || CollectionUtils.isEmpty(dto.getRoleIds())) {
            return false;
        }
        String operation = normalizeOperation(dto.getOperation());
        if (!Set.of("replace", "add", "remove").contains(operation)) {
            return false;
        }

        Set<Long> userIds = uniqueIds(dto.getUserIds());
        Set<Long> roleIds = uniqueIds(dto.getRoleIds());
        if (userIds.isEmpty() || roleIds.isEmpty()) {
            return false;
        }

        for (Long userId : userIds) {
            if (userService.getById(userId) == null) {
                return false;
            }
        }

        for (Long userId : userIds) {
            if ("replace".equals(operation)) {
                userRoleService.remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
                for (Long roleId : roleIds) {
                    UserRole userRole = new UserRole();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    userRoleService.save(userRole);
                }
            } else if ("add".equals(operation)) {
                for (Long roleId : roleIds) {
                    long count = userRoleService.count(new LambdaQueryWrapper<UserRole>()
                            .eq(UserRole::getUserId, userId)
                            .eq(UserRole::getRoleId, roleId));
                    if (count == 0) {
                        UserRole userRole = new UserRole();
                        userRole.setUserId(userId);
                        userRole.setRoleId(roleId);
                        userRoleService.save(userRole);
                    }
                }
            } else {
                userRoleService.remove(new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId)
                        .in(UserRole::getRoleId, roleIds));
            }
            applicationEventPublisher.publishEvent(new AdminUserRolesChangedEvent(userId, "batch_" + operation));
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean removeRole(Long userId, Long roleId) {
        if (userId == null || roleId == null) {
            return false;
        }
        boolean removed = userRoleService.remove(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, roleId));
        if (removed) {
            applicationEventPublisher.publishEvent(new AdminUserRolesChangedEvent(userId, "remove_role"));
        }
        return removed;
    }

    private String normalizeOperation(String operation) {
        if (!StringUtils.hasText(operation)) {
            return "replace";
        }
        return operation.trim().toLowerCase();
    }

    private Set<Long> uniqueIds(List<Long> ids) {
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        if (CollectionUtils.isEmpty(ids)) {
            return result;
        }
        for (Long id : ids) {
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }
}
