package com.huang.web.app.vo.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账号注销响应VO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "账号注销响应")
@Data
public class AccountCancelVO {

    @Schema(description = "注销申请ID")
    private Long cancelId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "注销类型：temporary-临时注销，permanent-永久注销")
    private String cancelType;

    @Schema(description = "注销状态：pending-待处理，approved-已批准，rejected-已拒绝")
    private String status;

    @Schema(description = "申请时间")
    private LocalDateTime applyTime;

    @Schema(description = "预计生效时间")
    private LocalDateTime effectiveTime;

    @Schema(description = "注销原因")
    private String reason;

    @Schema(description = "处理结果消息")
    private String message;

    @Schema(description = "是否可撤销")
    private Boolean cancellable;

    @Schema(description = "撤销截止时间")
    private LocalDateTime cancelDeadline;
}