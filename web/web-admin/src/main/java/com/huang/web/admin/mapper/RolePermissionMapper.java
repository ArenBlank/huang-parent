package com.huang.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huang.model.entity.RolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Role-permission mapping mapper.
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    @Delete("DELETE FROM role_permission WHERE role_id = #{roleId}")
    int deleteByRoleIdPhysical(@Param("roleId") Long roleId);

    @Delete({
            "<script>",
            "DELETE FROM role_permission",
            " WHERE role_id = #{roleId}",
            " <if test='permIds != null and permIds.size() > 0'>",
            "   AND perm_id IN",
            "   <foreach collection='permIds' item='pid' open='(' separator=',' close=')'>",
            "     #{pid}",
            "   </foreach>",
            " </if>",
            "</script>"
    })
    int deleteByRoleIdAndPermIdsPhysical(@Param("roleId") Long roleId,
                                        @Param("permIds") java.util.List<Long> permIds);
}
