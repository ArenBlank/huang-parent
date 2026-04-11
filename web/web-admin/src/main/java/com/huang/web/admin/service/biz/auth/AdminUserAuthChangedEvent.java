package com.huang.web.admin.service.biz.auth;

public record AdminUserAuthChangedEvent(Long userId, String reason) {
}
