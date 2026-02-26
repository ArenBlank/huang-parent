package com.huang.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huang.model.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色Mapper接口
 * @author system
 * @since 2026-02-25
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据角色编码查询角色
     * @param roleCode 角色编码
     * @return 角色信息
     */
    Role selectByRoleCode(@Param("roleCode") String roleCode);
}