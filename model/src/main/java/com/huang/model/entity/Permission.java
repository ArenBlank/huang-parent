package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限表
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Permission extends BaseEntity {

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限编码
     */
    private String permissionCode;

    /**
     * 权限类型: menu菜单 button按钮 api接口
     */
    private String permissionType;

    /**
     * 父权限ID
     */
    private Long parentId;

    /**
     * 资源路径(菜单路径/接口路径)
     */
    private String resourcePath;

    /**
     * 请求方法: GET POST PUT DELETE
     */
    private String resourceMethod;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态: 0禁用 1启用
     */
    private Integer status;
}
