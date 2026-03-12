package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.model.entity.Permission;
import com.huang.model.entity.Role;
import com.huang.model.entity.RolePermission;
import com.huang.web.admin.dto.role.RolePermissionAssignDTO;
import com.huang.web.admin.mapper.PermissionMapper;
import com.huang.web.admin.mapper.RolePermissionMapper;
import com.huang.web.admin.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminRolePermissionBizService {

    private final RoleService roleService;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    public AdminRolePermissionBizService(RoleService roleService,
                                         RolePermissionMapper rolePermissionMapper,
                                         PermissionMapper permissionMapper) {
        this.roleService = roleService;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
    }

    public List<Permission> listPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                .eq(Permission::getStatus, 1)
                .orderByAsc(Permission::getId));
    }

    public List<Permission> listPermissionsByRole(Long roleId) {
        if (roleId == null) {
            return new ArrayList<>();
        }
        List<RolePermission> links = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        if (links == null || links.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> permIds = links.stream()
                .map(RolePermission::getPermId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (permIds.isEmpty()) {
            return new ArrayList<>();
        }
        return permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                .in(Permission::getId, permIds)
                .eq(Permission::getStatus, 1));
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean assign(RolePermissionAssignDTO dto) {
        if (dto == null || dto.getRoleId() == null) {
            return false;
        }
        Role role = roleService.getById(dto.getRoleId());
        if (role == null) {
            return false;
        }
        String operation = normalizeOperation(dto.getOperation());
        List<String> permCodes = normalizeCodes(dto.getPermCodes());
        List<Long> permIds = resolvePermissionIds(permCodes);

        if ("replace".equals(operation)) {
            rolePermissionMapper.deleteByRoleIdPhysical(dto.getRoleId());
            insertRolePermissions(dto.getRoleId(), permIds);
            return true;
        }
        if ("add".equals(operation)) {
            insertRolePermissions(dto.getRoleId(), permIds);
            return true;
        }
        if ("remove".equals(operation)) {
            if (!permIds.isEmpty()) {
                rolePermissionMapper.deleteByRoleIdAndPermIdsPhysical(dto.getRoleId(), permIds);
            }
            return true;
        }
        return false;
    }

    private List<Long> resolvePermissionIds(List<String> permCodes) {
        if (permCodes == null || permCodes.isEmpty()) {
            return new ArrayList<>();
        }
        List<Permission> permissions = permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                .in(Permission::getPermCode, permCodes)
                .eq(Permission::getStatus, 1));
        if (permissions == null || permissions.isEmpty()) {
            return new ArrayList<>();
        }
        return permissions.stream()
                .map(Permission::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private void insertRolePermissions(Long roleId, List<Long> permIds) {
        if (permIds == null || permIds.isEmpty()) {
            return;
        }
        Set<Long> unique = new HashSet<>(permIds);
        for (Long permId : unique) {
            if (permId == null) {
                continue;
            }
            long count = rolePermissionMapper.selectCount(new LambdaQueryWrapper<RolePermission>()
                    .eq(RolePermission::getRoleId, roleId)
                    .eq(RolePermission::getPermId, permId));
            if (count > 0) {
                continue;
            }
            RolePermission link = new RolePermission();
            link.setRoleId(roleId);
            link.setPermId(permId);
            rolePermissionMapper.insert(link);
        }
    }

    private List<String> normalizeCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return new ArrayList<>();
        }
        return codes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private String normalizeOperation(String operation) {
        if (!StringUtils.hasText(operation)) {
            return "replace";
        }
        return operation.trim().toLowerCase();
    }
}
