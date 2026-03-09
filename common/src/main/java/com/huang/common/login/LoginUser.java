package com.huang.common.login;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
public class LoginUser {

    private Long userId;
    private String username;
    private Set<String> roleCodes = Collections.emptySet();

    public LoginUser(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public LoginUser(Long userId, String username, Set<String> roleCodes) {
        this.userId = userId;
        this.username = username;
        this.roleCodes = roleCodes == null ? Collections.emptySet() : new HashSet<>(roleCodes);
    }

    public boolean hasAnyRole(Collection<String> requiredRoleCodes) {
        if (requiredRoleCodes == null || requiredRoleCodes.isEmpty()) {
            return true;
        }
        if (roleCodes == null || roleCodes.isEmpty()) {
            return false;
        }
        for (String requiredRoleCode : requiredRoleCodes) {
            if (requiredRoleCode != null && roleCodes.contains(requiredRoleCode)) {
                return true;
            }
        }
        return false;
    }
}
