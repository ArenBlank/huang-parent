package com.huang.web.admin.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账号注销申请VO
 * @author system
 * @since 2025-01-24
 */
@Schema(description = "账号注销申请信息")
@Data
public class AccountCancelApplyVO {

    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "注销类型: temporary临时 permanent永久")
    private String cancelType;

    @Schema(description = "注销类型描述")
    private String cancelTypeDesc;

    @Schema(description = "注销原因")
    private String reason;

    @Schema(description = "申请时间")
    private LocalDateTime applyTime;

    @Schema(description = "状态: pending待审核 approved已批准 rejected已拒绝 cancelled已取消")
    private String status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;

    @Schema(description = "审核人ID")
    private Long reviewerId;

    @Schema(description = "审核人姓名")
    private String reviewerName;

    @Schema(description = "审核备注")
    private String reviewRemark;

    @Schema(description = "生效时间")
    private LocalDateTime effectiveTime;

    @Schema(description = "实际注销时间")
    private LocalDateTime actualCancelTime;

    @Schema(description = "撤销截止时间")
    private LocalDateTime cancelDeadline;

    @Schema(description = "是否可撤销: 0否 1是")
    private Integer isCancellable;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}