package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.result.Result;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.aop.OperationLog;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.dto.role.RoleCourseCategoryScopeBatchUpdateDTO;
import com.huang.web.admin.dto.role.RoleCourseCategoryScopeUpdateDTO;
import com.huang.model.entity.Role;
import com.huang.web.admin.dto.role.RoleStatusUpdateDTO;
import com.huang.web.admin.service.RoleService;
import com.huang.web.admin.service.biz.AdminRoleScopeBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin角色管理", description = "角色查询与状态维护")
@RestController
@RequestMapping("/admin/role")
@RequireAdminRole(AdminRoleCode.ADMIN)
public class RoleController {

    private final RoleService roleService;
    private final AdminRoleScopeBizService adminRoleScopeBizService;

    public RoleController(RoleService roleService,
                          AdminRoleScopeBizService adminRoleScopeBizService) {
        this.roleService = roleService;
        this.adminRoleScopeBizService = adminRoleScopeBizService;
    }

    @Operation(summary = "角色列表")
    @GetMapping("/list")
    public Result<List<Role>> list(@RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Role::getStatus, status);
        }
        wrapper.orderByDesc(Role::getId);
        return Result.ok(roleService.list(wrapper));
    }

    @Operation(summary = "角色详情")
    @GetMapping("/detail/{roleId}")
    public Result<Role> detail(@PathVariable Long roleId) {
        Role role = roleService.getById(roleId);
        return role == null ? Result.fail("角色不存在") : Result.ok(role);
    }

    @Operation(summary = "更新角色状态")
    @PutMapping("/status")
    public Result<String> updateStatus(@Valid @RequestBody RoleStatusUpdateDTO dto) {
        Role role = roleService.getById(dto.getRoleId());
        if (role == null) {
            return Result.fail("角色不存在");
        }
        role.setStatus(dto.getStatus());
        return roleService.updateById(role) ? Result.ok("更新成功") : Result.fail("更新失败");
    }

    @Operation(summary = "可用角色列表")
    @GetMapping("/available")
    public Result<List<Role>> available() {
        return Result.ok(roleService.list(new LambdaQueryWrapper<Role>()
                .eq(Role::getStatus, 1)
                .orderByAsc(Role::getId)));
    }

    @Operation(summary = "瑙掕壊璇剧▼鍒嗙被鑼冨洿鏌ヨ")
    @GetMapping("/{roleId}/course-category-scope")
    public Result<List<Long>> courseCategoryScope(@PathVariable Long roleId) {
        Role role = roleService.getById(roleId);
        if (role == null) {
            return Result.fail("瑙掕壊涓嶅瓨鍦?");
        }
        return Result.ok(adminRoleScopeBizService.listCourseCategoryScope(roleId));
    }

    @Operation(summary = "鏇存柊瑙掕壊璇剧▼鍒嗙被鑼冨洿")
    @OperationLog(module = "role_scope", action = "update", detail = "admin update role course category scope")
    @PutMapping("/{roleId}/course-category-scope")
    public Result<String> updateCourseCategoryScope(@PathVariable Long roleId,
                                                    @Valid @RequestBody RoleCourseCategoryScopeUpdateDTO dto) {
        boolean ok = adminRoleScopeBizService.updateCourseCategoryScope(roleId, dto.getCategoryIds());
        return ok ? Result.ok("鏇存柊鎴愬姛") : Result.fail("瑙掕壊涓嶅瓨鍦?");
    }

    @Operation(summary = "鎵归噺鏇存柊瑙掕壊璇剧▼鍒嗙被鑼冨洿")
    @OperationLog(module = "role_scope", action = "batch_update", detail = "admin batch update role course category scope")
    @PutMapping("/course-category-scope/batch")
    public Result<String> updateCourseCategoryScopeBatch(@Valid @RequestBody RoleCourseCategoryScopeBatchUpdateDTO dto) {
        boolean ok = adminRoleScopeBizService.updateCourseCategoryScopeBatch(dto.getItems());
        return ok ? Result.ok("鏇存柊鎴愬姛") : Result.fail("瑙掕壊涓嶅瓨鍦?");
    }
}
