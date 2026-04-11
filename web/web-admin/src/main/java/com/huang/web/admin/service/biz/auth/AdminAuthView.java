package com.huang.web.admin.service.biz.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminAuthView(Long userId,
                            String username,
                            String userType,
                            Integer status,
                            Integer tokenVersion,
                            Set<Long> roleIds,
                            Set<String> roleCodes,
                            Set<String> permissionCodes,
                            Set<Long> allowedCourseCategoryIds,
                            boolean adminAll) {
}
