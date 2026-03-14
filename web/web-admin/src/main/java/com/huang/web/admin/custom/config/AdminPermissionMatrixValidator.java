package com.huang.web.admin.custom.config;

import com.huang.model.entity.Permission;
import com.huang.web.admin.custom.annotation.RequireAdminPermission;
import com.huang.web.admin.mapper.PermissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AdminPermissionMatrixValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminPermissionMatrixValidator.class);

    private final RequestMappingHandlerMapping handlerMapping;
    private final PermissionMapper permissionMapper;
    private final AdminPermissionProperties permissionProperties;

    public AdminPermissionMatrixValidator(RequestMappingHandlerMapping handlerMapping,
                                          PermissionMapper permissionMapper,
                                          AdminPermissionProperties permissionProperties) {
        this.handlerMapping = handlerMapping;
        this.permissionMapper = permissionMapper;
        this.permissionProperties = permissionProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<String> required = collectRequiredPermissions();
        Set<String> configPerms = permissionProperties.allPermissions();
        Set<String> dbPerms = loadDbPermissions();

        Set<String> known = new HashSet<>(configPerms);
        known.addAll(dbPerms);

        Set<String> missing = new HashSet<>(required);
        missing.removeAll(known);

        log.info("Admin permission matrix: required={}, config={}, db={}, missing={}",
                required.size(), configPerms.size(), dbPerms.size(), missing.size());
        if (!missing.isEmpty()) {
            log.warn("Admin permissions missing from matrix: {}", missing);
        }
    }

    private Set<String> collectRequiredPermissions() {
        Set<String> required = new HashSet<>();
        for (HandlerMethod method : handlerMapping.getHandlerMethods().values()) {
            RequireAdminPermission require = method.getMethodAnnotation(RequireAdminPermission.class);
            if (require == null) {
                require = method.getBeanType().getAnnotation(RequireAdminPermission.class);
            }
            if (require != null && require.value().length > 0) {
                required.addAll(Arrays.asList(require.value()));
            }
        }
        return required;
    }

    private Set<String> loadDbPermissions() {
        Set<String> perms = new HashSet<>();
        try {
            List<Permission> list = permissionMapper.selectList(null);
            if (list != null) {
                for (Permission p : list) {
                    if (p != null && p.getPermissionCode() != null) {
                        perms.add(p.getPermissionCode());
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Skip permission DB scan: {}", ex.getMessage());
        }
        return perms;
    }
}
