package com.huang.web.admin.vo.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 账号统计信息VO
 */
@Data
@Schema(description = "账号统计信息")
public class AccountStatisticsVO {

    @Schema(description = "总用户数")
    private Long totalUsers;

    @Schema(description = "活跃用户数")
    private Long activeUsers;

    @Schema(description = "今日新增用户")
    private Long todayNewUsers;

    @Schema(description = "今日登录用户数")
    private Integer todayLoginUsers;

    @Schema(description = "今日登录次数")
    private Integer todayLogins;

    @Schema(description = "本周新增用户")
    private Long weekNewUsers;

    @Schema(description = "本月新增用户")
    private Long monthNewUsers;

    @Schema(description = "普通会员数")
    private Long normalMembers;

    @Schema(description = "VIP会员数")
    private Long vipMembers;

    @Schema(description = "教练数量")
    private Long coachCount;
}
