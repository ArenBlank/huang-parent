package com.huang.web.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.huang.web.admin.dto.coach.CoachResignationQueryDTO;
import com.huang.web.admin.dto.coach.CoachResignationReviewDTO;
import com.huang.web.admin.vo.coach.CoachResignationDetailVO;

/**
 * Admin端教练离职申请Service接口
 * @author system
 * @since 2025-01-24
 */
public interface CoachResignationService {

    /**
     * 分页查询教练离职申请列表
     */
    IPage<CoachResignationDetailVO> getResignationApplications(CoachResignationQueryDTO queryDTO);

    /**
     * 查看教练离职申请详情
     */
    CoachResignationDetailVO getResignationDetail(Long id);

    /**
     * 批准离职申请
     */
    boolean approveResignation(Long id, CoachResignationReviewDTO reviewDTO);

    /**
     * 拒绝离职申请
     */
    boolean rejectResignation(Long id, CoachResignationReviewDTO reviewDTO);

    /**
     * 取消离职申请（管理员操作）
     */
    boolean cancelResignation(Long id, String cancelReason);
}
