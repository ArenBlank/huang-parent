package com.huang.web.admin.service.biz.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Service
public class AdminAuthCacheService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthCacheService.class);

    static final long VIEW_TTL_SEC = 1800L;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AdminAuthViewLoader adminAuthViewLoader;

    public AdminAuthCacheService(StringRedisTemplate stringRedisTemplate,
                                 @Qualifier("adminAuthCacheObjectMapper") ObjectMapper objectMapper,
                                 AdminAuthViewLoader adminAuthViewLoader) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.adminAuthViewLoader = adminAuthViewLoader;
    }

    public AdminAuthView getOrLoad(Long userId) {
        if (userId == null) {
            return null;
        }
        String viewKey = AdminAuthCacheKeys.viewKey(userId);
        String json = stringRedisTemplate.opsForValue().get(viewKey);
        if (StringUtils.hasText(json)) {
            AdminAuthView cached = readView(json, userId);
            if (cached != null) {
                return cached;
            }
            stringRedisTemplate.delete(viewKey);
        }

        AdminAuthView loaded = adminAuthViewLoader.load(userId);
        if (loaded == null) {
            return null;
        }
        cacheView(loaded);
        return loaded;
    }

    public void cacheView(AdminAuthView view) {
        if (view == null || view.userId() == null) {
            return;
        }
        Long userId = view.userId();
        String userIdText = String.valueOf(userId);
        String userRolesKey = AdminAuthCacheKeys.userRolesKey(userId);

        Set<String> oldRoleIds = members(userRolesKey);
        if (!CollectionUtils.isEmpty(oldRoleIds)) {
            for (String oldRoleId : oldRoleIds) {
                if (!StringUtils.hasText(oldRoleId)) {
                    continue;
                }
                stringRedisTemplate.opsForSet().remove(AdminAuthCacheKeys.roleUsersKey(Long.valueOf(oldRoleId)), userIdText);
            }
        }

        stringRedisTemplate.opsForValue().set(
                AdminAuthCacheKeys.viewKey(userId),
                writeView(view),
                Duration.ofSeconds(VIEW_TTL_SEC)
        );

        stringRedisTemplate.delete(userRolesKey);
        if (CollectionUtils.isEmpty(view.roleIds())) {
            return;
        }

        for (Long roleId : view.roleIds()) {
            if (roleId == null) {
                continue;
            }
            String roleIdText = String.valueOf(roleId);
            stringRedisTemplate.opsForSet().add(userRolesKey, roleIdText);
            stringRedisTemplate.opsForSet().add(AdminAuthCacheKeys.roleUsersKey(roleId), userIdText);
        }
    }

    public void evictUser(Long userId) {
        if (userId == null) {
            return;
        }
        String userIdText = String.valueOf(userId);
        String userRolesKey = AdminAuthCacheKeys.userRolesKey(userId);
        Set<String> roleIds = members(userRolesKey);
        if (!CollectionUtils.isEmpty(roleIds)) {
            for (String roleId : roleIds) {
                if (!StringUtils.hasText(roleId)) {
                    continue;
                }
                stringRedisTemplate.opsForSet().remove(AdminAuthCacheKeys.roleUsersKey(Long.valueOf(roleId)), userIdText);
            }
        }
        stringRedisTemplate.delete(AdminAuthCacheKeys.viewKey(userId));
        stringRedisTemplate.delete(userRolesKey);
    }

    public void evictUsersByRole(Long roleId) {
        if (roleId == null) {
            return;
        }
        String roleUsersKey = AdminAuthCacheKeys.roleUsersKey(roleId);
        Set<String> userIds = members(roleUsersKey);
        if (!CollectionUtils.isEmpty(userIds)) {
            for (String userId : userIds) {
                if (!StringUtils.hasText(userId)) {
                    continue;
                }
                evictUser(Long.valueOf(userId));
            }
        }
        stringRedisTemplate.delete(roleUsersKey);
    }

    private Set<String> members(String key) {
        Set<String> members = stringRedisTemplate.opsForSet().members(key);
        if (CollectionUtils.isEmpty(members)) {
            return Set.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        members.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(Objects::nonNull)
                .forEach(result::add);
        return result;
    }

    private AdminAuthView readView(String json, Long userId) {
        try {
            return objectMapper.readValue(json, AdminAuthView.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to read cached admin auth view for user {}, cache will be rebuilt", userId, e);
            return null;
        }
    }

    private String writeView(AdminAuthView view) {
        try {
            return objectMapper.writeValueAsString(view);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize admin auth view for user " + view.userId(), e);
        }
    }
}
