package com.huang.web.admin.service.biz.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AdminAuthCacheEvictionListener {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthCacheEvictionListener.class);

    private final AdminAuthCacheService adminAuthCacheService;

    public AdminAuthCacheEvictionListener(AdminAuthCacheService adminAuthCacheService) {
        this.adminAuthCacheService = adminAuthCacheService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAdminUserAuthChanged(AdminUserAuthChangedEvent event) {
        if (event == null || event.userId() == null) {
            return;
        }
        log.debug("Evicting admin auth cache for user {} after auth change, reason={}", event.userId(), event.reason());
        adminAuthCacheService.evictUser(event.userId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAdminUserRolesChanged(AdminUserRolesChangedEvent event) {
        if (event == null || event.userId() == null) {
            return;
        }
        log.debug("Evicting admin auth cache for user {} after role change, reason={}", event.userId(), event.reason());
        adminAuthCacheService.evictUser(event.userId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAdminRoleAclChanged(AdminRoleAclChangedEvent event) {
        if (event == null || event.roleId() == null) {
            return;
        }
        log.debug("Evicting admin auth caches for role {} after ACL change, reason={}", event.roleId(), event.reason());
        adminAuthCacheService.evictUsersByRole(event.roleId());
    }
}
