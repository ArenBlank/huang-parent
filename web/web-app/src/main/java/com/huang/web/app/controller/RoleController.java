package com.huang.web.app.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.common.result.Result;
import com.huang.common.result.ResultCodeEnum;
import com.huang.model.entity.Role;
import com.huang.web.app.dto.role.RoleQueryDTO;
import com.huang.web.app.vo.role.RoleListVO;
import com.huang.web.app.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * App端角色管理控制器
 * @author system
 * @since 2025-01-24
 */
@Tag(name = "App端角色管理", description = "用户角色查询相关接口")
@RestController
@RequestMapping("/app/role")
@Validated
@Slf4j
public class RoleController {

    @Autowired
    private RoleService roleService;

    /**
     * 角色列表查询
     */
    @Operation(summary = "角色列表查询", description = "支持分页和多条件筛选的角色列表查询")
    @GetMapping("/list")
    public Result<Page<RoleListVO>> getRoleList(@Valid RoleQueryDTO queryDTO) {
        try {
            // 执行查询
            Page<Role> rolePage = roleService.getPageList(queryDTO);
            
            // 转换为VO
            Page<RoleListVO> resultPage = new Page<>();
            BeanUtils.copyProperties(rolePage, resultPage);
            
            List<RoleListVO> roleListVOs = rolePage.getRecords().stream().map(role -> {
                RoleListVO vo = new RoleListVO();
                BeanUtils.copyProperties(role, vo);
                
                // 设置状态描述
                vo.setStatusDesc(getStatusDesc(role.getStatus()));
                
                return vo;
            }).collect(Collectors.toList());
            
            resultPage.setRecords(roleListVOs);
            
            return Result.ok(resultPage);
            
        } catch (Exception e) {
            log.error("查询角色列表失败", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "查询角色列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有可用角色（用于下拉选择）
     */
    @Operation(summary = "获取可用角色列表", description = "获取所有启用状态的角色，用于下拉选择")
    @GetMapping("/available")
    public Result<List<RoleListVO>> getAvailableRoles() {
        try {
            List<Role> roles = roleService.lambdaQuery()
                    .eq(Role::getStatus, 1)
                    .orderByAsc(Role::getRoleName)
                    .list();
            
            List<RoleListVO> roleVOs = roles.stream().map(role -> {
                RoleListVO vo = new RoleListVO();
                BeanUtils.copyProperties(role, vo);
                vo.setStatusDesc(getStatusDesc(role.getStatus()));
                return vo;
            }).collect(Collectors.toList());
            
            return Result.ok(roleVOs);
            
        } catch (Exception e) {
            log.error("获取可用角色列表失败", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "获取可用角色列表失败：" + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) return "未知";
        return status == 1 ? "正常" : "禁用";
    }
}