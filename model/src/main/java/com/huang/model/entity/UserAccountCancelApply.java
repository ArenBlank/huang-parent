package com.huang.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户账号注销申请实体
 * @author system
 * @since 2025-01-24
 */
@Data
@TableName("user_account_cancel_apply")
public class UserAccountCancelApply {

    /**
     * 申请ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 注销类型: temporary临时 permanent永久
     */
    private String cancelType;

    /**
     * 注销原因
     */
    private String reason;

    /**
     * 申请时间
     */
    private LocalDateTime applyTime;

    /**
     * 状态: pending待审核 approved已批准 rejected已拒绝 cancelled已取消
     */
    private String status;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;

    /**
     * 审核人ID
     */
    private Long reviewerId;

    /**
     * 审核备注
     */
    private String reviewRemark;

    /**
     * 生效时间
     */
    private LocalDateTime effectiveTime;

    /**
     * 实际注销时间
     */
    private LocalDateTime actualCancelTime;

    /**
     * 撤销截止时间
     */
    private LocalDateTime cancelDeadline;

    /**
     * 是否可撤销: 0否 1是
     */
    private Integer isCancellable;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否删除: 0否 1是
     */
    @TableLogic
    private Integer isDeleted;
}