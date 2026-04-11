package com.huang.web.admin.mapper;

import com.huang.model.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AdminAuthViewQueryMapper {

    List<Role> selectActiveRolesByUserId(@Param("userId") Long userId);

    List<String> selectPermissionCodesByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    List<Long> selectCategoryIdsByRoleIds(@Param("roleIds") Collection<Long> roleIds);
}
