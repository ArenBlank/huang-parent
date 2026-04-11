package com.huang.web.admin.service.biz.auth;

public final class AdminAuthCacheKeys {

    private static final String VIEW_KEY_PREFIX = "admin:auth:view:";
    private static final String USER_ROLES_KEY_PREFIX = "admin:auth:user:roles:";
    private static final String ROLE_USERS_KEY_PREFIX = "admin:auth:role:users:";

    private AdminAuthCacheKeys() {
    }

    public static String viewKey(Long userId) {
        return VIEW_KEY_PREFIX + userId;
    }

    public static String userRolesKey(Long userId) {
        return USER_ROLES_KEY_PREFIX + userId;
    }

    public static String roleUsersKey(Long roleId) {
        return ROLE_USERS_KEY_PREFIX + roleId;
    }
}
