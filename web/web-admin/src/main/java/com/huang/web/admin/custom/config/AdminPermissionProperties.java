package com.huang.web.admin.custom.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "admin.permission")
public class AdminPermissionProperties {

    private boolean adminAll = true;

    private Map<String, Set<String>> roleMap = new HashMap<>();

    private Map<String, Set<Long>> courseCategoryScope = new HashMap<>();

    public boolean isAdminAll() {
        return adminAll;
    }

    public void setAdminAll(boolean adminAll) {
        this.adminAll = adminAll;
    }

    public Map<String, Set<String>> getRoleMap() {
        return roleMap;
    }

    public void setRoleMap(Map<String, Set<String>> roleMap) {
        this.roleMap = roleMap == null ? new HashMap<>() : roleMap;
    }

    public Map<String, Set<Long>> getCourseCategoryScope() {
        return courseCategoryScope;
    }

    public void setCourseCategoryScope(Map<String, Set<Long>> courseCategoryScope) {
        this.courseCategoryScope = courseCategoryScope == null ? new HashMap<>() : courseCategoryScope;
    }

    public Set<String> permissionsFor(String roleCode) {
        return roleMap.getOrDefault(roleCode, new HashSet<>());
    }

    public Set<Long> courseCategoriesFor(String roleCode) {
        return courseCategoryScope.getOrDefault(roleCode, new HashSet<>());
    }
}
