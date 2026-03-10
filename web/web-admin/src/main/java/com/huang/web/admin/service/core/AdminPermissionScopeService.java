package com.huang.web.admin.service.core;

import com.huang.common.exception.HuangException;
import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.common.result.ResultCodeEnum;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.config.AdminPermissionProperties;
import com.huang.web.admin.service.biz.AdminOperationLogBizService;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class AdminPermissionScopeService {

    private static final Long ALL_CATEGORY = -1L;

    private final AdminPermissionProperties adminPermissionProperties;
    private final AdminOperationLogBizService adminOperationLogBizService;

    public AdminPermissionScopeService(AdminPermissionProperties adminPermissionProperties,
                                       AdminOperationLogBizService adminOperationLogBizService) {
        this.adminPermissionProperties = adminPermissionProperties;
        this.adminOperationLogBizService = adminOperationLogBizService;
    }

    public void assertCourseCategoryAccess(Long categoryId, String action, String detail) {
        if (categoryId == null) {
            return;
        }
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return;
        }
        Set<String> roleCodes = loginUser.getRoleCodes();
        if (adminPermissionProperties.isAdminAll() && roleCodes.contains(AdminRoleCode.ADMIN)) {
            return;
        }
        Set<Long> allowed = new HashSet<>();
        for (String roleCode : roleCodes) {
            allowed.addAll(adminPermissionProperties.courseCategoriesFor(roleCode));
        }
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
}
