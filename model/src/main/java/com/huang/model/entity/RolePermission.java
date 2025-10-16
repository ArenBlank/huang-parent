package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色权限关联表
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RolePermission extends BaseEntity {

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 权限ID
     */
    private Long permissionId;
}
