package com.huang.web.admin.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 用户统计视图对象
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "用户统计视图")
@Data
public class UserStatisticsVO {

    @Schema(description = "总用户数")
    private Long totalUsers;

    @Schema(description = "正常状态用户数")
    private Long activeUsers;

    @Schema(description = "禁用状态用户数")
    private Long inactiveUsers;

    @Schema(description = "今日新增用户数")
    private Long todayNewUsers;

    @Schema(description = "本周新增用户数")
    private Long weekNewUsers;

    @Schema(description = "本月新增用户数")
    private Long monthNewUsers;

    @Schema(description = "性别分布统计")
    private List<GenderStatistic> genderStatistics;

    @Schema(description = "角色分布统计")
    private List<RoleStatistic> roleStatistics;

    @Schema(description = "最近7天用户注册趋势")
    private List<DailyStatistic> dailyRegistrationTrend;

    @Schema(description = "最近7天用户登录趋势")
    private List<DailyStatistic> dailyLoginTrend;

    @Schema(description = "性别统计")
    @Data
    public static class GenderStatistic {
        
        @Schema(description = "性别：0-未知，1-男，2-女")
        private Integer gender;
        
        @Schema(description = "性别描述")
        private String genderDesc;
        
        @Schema(description = "用户数量")
        private Long count;
        
        @Schema(description = "占比")
        private Double percentage;
    }

    @Schema(description = "角色统计")
    @Data
    public static class RoleStatistic {
        
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

    @Schema(description = "日统计")
    @Data
    public static class DailyStatistic {
        
        @Schema(description = "日期")
        private String date;
        
        @Schema(description = "数量")
        private Long count;
    }
}