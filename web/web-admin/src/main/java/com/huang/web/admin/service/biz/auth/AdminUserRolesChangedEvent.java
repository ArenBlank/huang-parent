package com.huang.web.admin.service.biz.auth;

public record AdminUserRolesChangedEvent(Long userId, String reason) {
}
