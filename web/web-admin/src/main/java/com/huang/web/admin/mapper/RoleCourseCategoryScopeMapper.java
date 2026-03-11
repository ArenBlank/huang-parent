package com.huang.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huang.model.entity.RoleCourseCategoryScope;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface RoleCourseCategoryScopeMapper extends BaseMapper<RoleCourseCategoryScope> {

    @Delete("DELETE FROM role_course_category_scope WHERE role_id = #{roleId}")
    int deleteByRoleIdPhysical(@Param("roleId") Long roleId);
}
