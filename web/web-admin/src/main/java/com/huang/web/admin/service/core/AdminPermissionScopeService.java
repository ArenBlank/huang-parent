package com.huang.web.admin.service.core;

import com.huang.common.exception.HuangException;
import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.common.result.ResultCodeEnum;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.config.AdminPermissionProperties;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.model.entity.Role;
import com.huang.model.entity.RoleCourseCategoryScope;
import com.huang.web.admin.mapper.RoleCourseCategoryScopeMapper;
import com.huang.web.admin.service.RoleService;
import com.huang.web.admin.service.biz.AdminOperationLogBizService;
import com.huang.web.admin.service.biz.auth.AdminAuthView;
import com.huang.web.admin.service.biz.auth.AdminAuthViewHolder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class AdminPermissionScopeService {

    private static final Long ALL_CATEGORY = -1L;

    private final AdminPermissionProperties adminPermissionProperties;
    private final AdminOperationLogBizService adminOperationLogBizService;
    private final RoleService roleService;
    private final RoleCourseCategoryScopeMapper roleCourseCategoryScopeMapper;

    public AdminPermissionScopeService(AdminPermissionProperties adminPermissionProperties,
                                       AdminOperationLogBizService adminOperationLogBizService,
                                       RoleService roleService,
                                       RoleCourseCategoryScopeMapper roleCourseCategoryScopeMapper) {
        this.adminPermissionProperties = adminPermissionProperties;
        this.adminOperationLogBizService = adminOperationLogBizService;
        this.roleService = roleService;
        this.roleCourseCategoryScopeMapper = roleCourseCategoryScopeMapper;
    }

    public void assertCourseCategoryAccess(Long categoryId, String action, String detail) {
        if (categoryId == null) {
            return;
        }
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return;
        }
        AdminAuthView authView = AdminAuthViewHolder.get();
        if (authView != null) {
            if (authView.adminAll()) {
                return;
            }
            Set<Long> allowed = authView.allowedCourseCategoryIds();
            if (allowed == null || allowed.isEmpty()) {
                return;
            }
            if (allowed.contains(ALL_CATEGORY) || allowed.contains(categoryId)) {
                return;
            }
            String actionLabel = (action == null || action.isBlank()) ? "course" : action;
            String detailLabel = "categoryId=" + categoryId + (detail == null ? "" : (", " + detail));
            adminOperationLogBizService.record("authz", actionLabel, detailLabel, false);
            throw new HuangException(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN);
        }
        Set<String> roleCodes = loginUser.getRoleCodes();
        if (adminPermissionProperties.isAdminAll() && roleCodes.contains(AdminRoleCode.ADMIN)) {
            return;
        }
        Set<Long> allowed = resolveAllowedCategories(roleCodes);
        if (allowed.isEmpty()) {
            return;
        }
        if (allowed.contains(ALL_CATEGORY) || allowed.contains(categoryId)) {
            return;
        }
        String actionLabel = (action == null || action.isBlank()) ? "course" : action;
        String detailLabel = "categoryId=" + categoryId + (detail == null ? "" : (", " + detail));
        adminOperationLogBizService.record("authz", actionLabel, detailLabel, false);
        throw new HuangException(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN);
    }

    private Set<Long> resolveAllowedCategories(Set<String> roleCodes) {
        Set<Long> allowed = loadAllowedCategoriesFromDb(roleCodes);
        if (!allowed.isEmpty()) {
            return allowed;
        }
        for (String roleCode : roleCodes) {
            allowed.addAll(adminPermissionProperties.courseCategoriesFor(roleCode));
        }
        return allowed;
    }

    private Set<Long> loadAllowedCategoriesFromDb(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return new HashSet<>();
        }
        Set<Role> roles = new HashSet<>(roleService.list(new LambdaQueryWrapper<Role>()
                .in(Role::getRoleCode, roleCodes)));
        if (roles.isEmpty()) {
            return new HashSet<>();
        }
        Set<Long> roleIds = new HashSet<>();
        for (Role role : roles) {
            if (role != null && role.getId() != null) {
                roleIds.add(role.getId());
            }
        }
        if (roleIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Long> categories = new HashSet<>();
        for (RoleCourseCategoryScope scope : roleCourseCategoryScopeMapper.selectList(
                new LambdaQueryWrapper<RoleCourseCategoryScope>().in(RoleCourseCategoryScope::getRoleId, roleIds))) {
            if (scope != null && scope.getCategoryId() != null) {
                categories.add(scope.getCategoryId());
            }
        }
        return categories;
    }
}
