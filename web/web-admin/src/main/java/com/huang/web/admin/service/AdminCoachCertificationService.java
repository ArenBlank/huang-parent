package com.huang.web.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.model.entity.CoachCertificationApply;
import com.huang.web.admin.dto.coach.CoachCertificationQueryDTO;
import com.huang.web.admin.dto.coach.CoachCertificationReviewDTO;
import com.huang.web.admin.vo.coach.CoachCertificationDetailVO;
import com.huang.web.admin.vo.coach.CoachCertificationListVO;

/**
 * Admin端教练认证申请Service接口
 * @author system
 * @since 2025-01-24
 */
public interface AdminCoachCertificationService {

    /**
     * 分页查询教练认证申请列表
     */
    Page<CoachCertificationListVO> getApplicationList(CoachCertificationQueryDTO queryDTO);

    /**
     * 获取教练认证申请详情
     */
    CoachCertificationDetailVO getApplicationDetail(Long id);

    /**
     * 审核教练认证申请
     */
    boolean reviewApplication(CoachCertificationReviewDTO reviewDTO);

    /**
     * 创建教练记录并分配角色
     */
    void createCoachAndAssignRole(CoachCertificationApply application, String certificationNo);

    /**
     * 智能升级用户角色为教练
     */
    void upgradeUserRoleToCoach(Long userId);
}
