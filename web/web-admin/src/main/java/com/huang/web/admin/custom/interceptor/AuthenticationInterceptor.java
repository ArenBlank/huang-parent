package com.huang.web.admin.custom.interceptor;

import com.huang.common.exception.HuangException;
import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.common.result.ResultCodeEnum;
import com.huang.common.utils.JwtUtil;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminPermission;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.custom.config.AdminPermissionProperties;
import com.huang.web.admin.service.biz.AdminOperationLogBizService;
import com.huang.web.admin.service.biz.auth.AdminAuthCacheService;
import com.huang.web.admin.service.biz.auth.AdminAuthView;
import com.huang.web.admin.service.biz.auth.AdminAuthViewHolder;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final AdminAuthCacheService adminAuthCacheService;
    private final AdminPermissionProperties adminPermissionProperties;
    private final AdminOperationLogBizService adminOperationLogBizService;

    public AuthenticationInterceptor(AdminAuthCacheService adminAuthCacheService,
                                     AdminPermissionProperties adminPermissionProperties,
                                     AdminOperationLogBizService adminOperationLogBizService) {
        this.adminAuthCacheService = adminAuthCacheService;
        this.adminPermissionProperties = adminPermissionProperties;
        this.adminOperationLogBizService = adminOperationLogBizService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            token = request.getHeader("access-token");
        }
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (!StringUtils.hasText(token)) {
            throw new HuangException(ResultCodeEnum.ADMIN_LOGIN_AUTH);
        }

        Claims claims = JwtUtil.parseToken(token);
        String platform = claims.get("platform", String.class);
        if (!"admin".equalsIgnoreCase(platform)) {
            throw new HuangException(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN);
        }
        Long userId = claims.get("userId", Long.class);
        String username = claims.get("username", String.class);
        if (userId == null || !StringUtils.hasText(username)) {
            throw new HuangException(ResultCodeEnum.TOKEN_INVALID);
        }

        AdminAuthView authView = adminAuthCacheService.getOrLoad(userId);
        if (authView == null || authView.status() == null || authView.status() != 1) {
            throw new HuangException(ResultCodeEnum.ADMIN_LOGIN_AUTH);
        }
        if (!"admin".equalsIgnoreCase(authView.userType())) {
            throw new HuangException(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN);
        }
        if (!Objects.equals(normalizeTokenVersion(authView.tokenVersion()), JwtUtil.getTokenVersionFromClaims(claims))) {
            throw new HuangException(ResultCodeEnum.TOKEN_INVALID);
        }

        String resolvedUsername = StringUtils.hasText(authView.username()) ? authView.username() : username;
        Set<String> roleCodes = authView.roleCodes();
        LoginUser loginUser = new LoginUser(userId, resolvedUsername, roleCodes);
        LoginUserHolder.setLoginUser(loginUser);
        AdminAuthViewHolder.set(authView);

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireAdminRole requireAdminRole = handlerMethod.getMethodAnnotation(RequireAdminRole.class);
        if (requireAdminRole == null) {
            requireAdminRole = handlerMethod.getBeanType().getAnnotation(RequireAdminRole.class);
        }
        if (requireAdminRole != null && !loginUser.hasAnyRole(Arrays.asList(requireAdminRole.value()))) {
            recordAccessDenied("role_deny", request, "required=" + Arrays.toString(requireAdminRole.value()));
            throw new HuangException(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN);
        }

        RequireAdminPermission requireAdminPermission = handlerMethod.getMethodAnnotation(RequireAdminPermission.class);
        if (requireAdminPermission == null) {
            requireAdminPermission = handlerMethod.getBeanType().getAnnotation(RequireAdminPermission.class);
        }
        if (requireAdminPermission != null && !hasPermission(authView, requireAdminPermission.value())) {
            recordAccessDenied("perm_deny", request, "required=" + Arrays.toString(requireAdminPermission.value()));
            throw new HuangException(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        LoginUserHolder.clear();
        AdminAuthViewHolder.clear();
    }

    private boolean hasPermission(AdminAuthView authView, String[] required) {
        if (required == null || required.length == 0) {
            return true;
        }
        if (authView == null) {
            return false;
        }
        Set<String> roleCodes = authView.roleCodes();
        if (roleCodes == null || roleCodes.isEmpty()) {
            return false;
        }
        if (authView.adminAll() || (adminPermissionProperties.isAdminAll() && roleCodes.contains(AdminRoleCode.ADMIN))) {
            return true;
        }
        Set<String> allowed = authView.permissionCodes();
        if (allowed == null || allowed.isEmpty()) {
            return false;
        }
        if (allowed.contains("*")) {
            return true;
        }
        for (String perm : required) {
            if (allowed.contains(perm)) {
                return true;
            }
        }
        return false;
    }

    private void recordAccessDenied(String action, HttpServletRequest request, String detail) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String detailText = "method=" + method + ", uri=" + uri + (detail == null ? "" : (", " + detail));
        adminOperationLogBizService.record("authz", action, detailText, false);
    }

    private int normalizeTokenVersion(Integer tokenVersion) {
        return tokenVersion == null || tokenVersion < 0 ? 0 : tokenVersion;
    }
}
