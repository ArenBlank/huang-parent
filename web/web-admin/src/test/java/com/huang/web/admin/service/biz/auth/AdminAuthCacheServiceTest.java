package com.huang.web.admin.service.biz.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huang.web.admin.custom.config.AdminAuthCacheConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthCacheServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private AdminAuthViewLoader adminAuthViewLoader;

    private ObjectMapper objectMapper;

    private AdminAuthCacheService adminAuthCacheService;

    @BeforeEach
    void setUp() {
        objectMapper = new AdminAuthCacheConfiguration().adminAuthCacheObjectMapper();
        adminAuthCacheService = new AdminAuthCacheService(stringRedisTemplate, objectMapper, adminAuthViewLoader);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void getOrLoad_shouldReturnCachedViewWhenJsonContainsUnknownFields() throws Exception {
        AdminAuthView view = sampleView(1L, Set.of(10L, 20L));
        String json = objectMapper.writeValueAsString(Map.ofEntries(
                new SimpleEntry<>("userId", view.userId()),
                new SimpleEntry<>("username", view.username()),
                new SimpleEntry<>("userType", view.userType()),
                new SimpleEntry<>("status", view.status()),
                new SimpleEntry<>("tokenVersion", view.tokenVersion()),
                new SimpleEntry<>("roleIds", view.roleIds()),
                new SimpleEntry<>("roleCodes", view.roleCodes()),
                new SimpleEntry<>("permissionCodes", view.permissionCodes()),
                new SimpleEntry<>("allowedCourseCategoryIds", view.allowedCourseCategoryIds()),
                new SimpleEntry<>("adminAll", view.adminAll()),
                new SimpleEntry<>("unknownField", "ignored")
        ));
        when(valueOperations.get(AdminAuthCacheKeys.viewKey(1L))).thenReturn(json);

        AdminAuthView cached = adminAuthCacheService.getOrLoad(1L);

        assertNotNull(cached);
        assertEquals(view.userId(), cached.userId());
        assertEquals(view.roleIds(), cached.roleIds());
        verify(adminAuthViewLoader, never()).load(any());
    }

    @Test
    void getOrLoad_shouldLoadAndCacheWhenCacheMiss() {
        AdminAuthView view = sampleView(1L, Set.of(10L, 20L));
        when(valueOperations.get(AdminAuthCacheKeys.viewKey(1L))).thenReturn(null);
        when(setOperations.members(AdminAuthCacheKeys.userRolesKey(1L))).thenReturn(Set.of());
        when(adminAuthViewLoader.load(1L)).thenReturn(view);

        AdminAuthView loaded = adminAuthCacheService.getOrLoad(1L);

        assertNotNull(loaded);
        assertEquals(1L, loaded.userId());
        verify(valueOperations).set(eq(AdminAuthCacheKeys.viewKey(1L)), anyString(), eq(Duration.ofSeconds(AdminAuthCacheService.VIEW_TTL_SEC)));
        verify(stringRedisTemplate).delete(AdminAuthCacheKeys.userRolesKey(1L));
        verify(setOperations).add(AdminAuthCacheKeys.userRolesKey(1L), "10");
        verify(setOperations).add(AdminAuthCacheKeys.userRolesKey(1L), "20");
        verify(setOperations).add(AdminAuthCacheKeys.roleUsersKey(10L), "1");
        verify(setOperations).add(AdminAuthCacheKeys.roleUsersKey(20L), "1");
    }

    @Test
    void cacheView_shouldRemoveOldRoleBindingsBeforeRebuildingIndexes() {
        AdminAuthView view = sampleView(3L, Set.of(30L, 40L));
        when(setOperations.members(AdminAuthCacheKeys.userRolesKey(3L))).thenReturn(new LinkedHashSet<>(Set.of("10", "20")));

        adminAuthCacheService.cacheView(view);

        verify(setOperations).remove(AdminAuthCacheKeys.roleUsersKey(10L), "3");
        verify(setOperations).remove(AdminAuthCacheKeys.roleUsersKey(20L), "3");
        verify(valueOperations).set(eq(AdminAuthCacheKeys.viewKey(3L)), anyString(), eq(Duration.ofSeconds(AdminAuthCacheService.VIEW_TTL_SEC)));
        verify(stringRedisTemplate).delete(AdminAuthCacheKeys.userRolesKey(3L));
        verify(setOperations).add(AdminAuthCacheKeys.userRolesKey(3L), "30");
        verify(setOperations).add(AdminAuthCacheKeys.userRolesKey(3L), "40");
        verify(setOperations).add(AdminAuthCacheKeys.roleUsersKey(30L), "3");
        verify(setOperations).add(AdminAuthCacheKeys.roleUsersKey(40L), "3");
    }

    @Test
    void evictUser_shouldDeleteViewAndReverseIndexes() {
        when(setOperations.members(AdminAuthCacheKeys.userRolesKey(5L))).thenReturn(Set.of("100", "200"));

        adminAuthCacheService.evictUser(5L);

        verify(setOperations).remove(AdminAuthCacheKeys.roleUsersKey(100L), "5");
        verify(setOperations).remove(AdminAuthCacheKeys.roleUsersKey(200L), "5");
        verify(stringRedisTemplate).delete(AdminAuthCacheKeys.viewKey(5L));
        verify(stringRedisTemplate).delete(AdminAuthCacheKeys.userRolesKey(5L));
    }

    @Test
    void evictUsersByRole_shouldEvictEachBoundUser() {
        when(setOperations.members(AdminAuthCacheKeys.roleUsersKey(8L))).thenReturn(Set.of("1", "2"));
        when(setOperations.members(AdminAuthCacheKeys.userRolesKey(1L))).thenReturn(Set.of("8"));
        when(setOperations.members(AdminAuthCacheKeys.userRolesKey(2L))).thenReturn(Set.of("8", "9"));

        adminAuthCacheService.evictUsersByRole(8L);

        verify(stringRedisTemplate).delete(AdminAuthCacheKeys.viewKey(1L));
        verify(stringRedisTemplate).delete(AdminAuthCacheKeys.viewKey(2L));
        verify(setOperations).remove(AdminAuthCacheKeys.roleUsersKey(8L), "1");
        verify(setOperations).remove(AdminAuthCacheKeys.roleUsersKey(8L), "2");
        verify(setOperations).remove(AdminAuthCacheKeys.roleUsersKey(9L), "2");
        verify(stringRedisTemplate).delete(AdminAuthCacheKeys.roleUsersKey(8L));
    }

    @Test
    void cacheView_shouldSkipNullRoleIds() {
        LinkedHashSet<Long> roleIds = new LinkedHashSet<>();
        roleIds.add(90L);
        roleIds.add(null);
        AdminAuthView view = new AdminAuthView(
                9L,
                "user9",
                "admin",
                1,
                0,
                roleIds,
                new LinkedHashSet<>(Set.of("OPS_ADMIN")),
                new LinkedHashSet<>(Set.of("dashboard:read")),
                new LinkedHashSet<>(Set.of(1L)),
                false
        );
        when(setOperations.members(AdminAuthCacheKeys.userRolesKey(9L))).thenReturn(Set.of());

        adminAuthCacheService.cacheView(view);

        verify(setOperations).add(AdminAuthCacheKeys.userRolesKey(9L), "90");
        verify(setOperations).add(AdminAuthCacheKeys.roleUsersKey(90L), "9");
    }

    private AdminAuthView sampleView(Long userId, Set<Long> roleIds) {
        LinkedHashSet<Long> stableRoleIds = new LinkedHashSet<>(roleIds);
        LinkedHashSet<String> stableRoleCodes = new LinkedHashSet<>();
        for (Long roleId : stableRoleIds) {
            stableRoleCodes.add("ROLE_" + roleId);
        }
        return new AdminAuthView(
                userId,
                "admin-" + userId,
                "admin",
                1,
                2,
                stableRoleIds,
                stableRoleCodes,
                new LinkedHashSet<>(Set.of("dashboard:read", "course:update")),
                new LinkedHashSet<>(Set.of(1L, 2L)),
                true
        );
    }
}
