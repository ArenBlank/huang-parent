package com.huang.web.admin.vo.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 角色统计视图对象
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "角色统计视图")
@Data
public class RoleStatisticsVO {

    @Schema(description = "总角色数")
    private Long totalRoles;

    @Schema(description = "正常状态角色数")
    private Long activeRoles;

    @Schema(description = "禁用状态角色数")
    private Long inactiveRoles;

    @Schema(description = "角色用户分布统计")
    private List<RoleUserStatistic> roleUserStatistics;

    @Schema(description = "角色状态分布统计")
    private List<RoleStatusStatistic> roleStatusStatistics;

    @Schema(description = "角色用户统计")
    @Data
    public static class RoleUserStatistic {
        
        @Schema(description = "角色ID")
        private Long roleId;
        
        @Schema(description = "角色名称")
        private String roleName;
        
        @Schema(description = "角色编码")
        private String roleCode;
        
        @Schema(description = "用户数量")
        private Long userCount;
        
        @Schema(description = "占比")
        private Double percentage;
    }

    @Schema(description = "角色状态统计")
    @Data
    public static class RoleStatusStatistic {
        
        @Schema(description = "状态：0-禁用，1-正常")
        private Integer status;
        
        @Schema(description = "状态描述")
        private String statusDesc;
        
        @Schema(description = "角色数量")
        private Long count;
        
        @Schema(description = "占比")
        private Double percentage;
    }
}