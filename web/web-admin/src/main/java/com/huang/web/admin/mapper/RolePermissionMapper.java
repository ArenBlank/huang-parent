package com.huang.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huang.model.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色权限关联 Mapper
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}
