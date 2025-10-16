package com.huang.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huang.model.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限 Mapper
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据角色ID查询权限列表
     */
    @Select("SELECT p.* FROM permission p " +
            "INNER JOIN role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId} " +
            "AND p.is_deleted = 0 " +
            "AND rp.is_deleted = 0 " +
            "AND p.status = 1")
    List<Permission> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据用户ID查询权限列表
     */
    @Select("SELECT DISTINCT p.* FROM permission p " +
            "INNER JOIN role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} " +
            "AND p.is_deleted = 0 " +
            "AND rp.is_deleted = 0 " +
            "AND ur.is_deleted = 0 " +
            "AND p.status = 1")
    List<Permission> selectByUserId(@Param("userId") Long userId);
}
