package com.huang.web.admin.service.biz.auth;

public record AdminRoleAclChangedEvent(Long roleId, String reason) {
}
