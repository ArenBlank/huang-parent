package com.huang.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.huang.dto.AdminCoachScheduleQueryDTO;
import com.huang.dto.CoachScheduleReviewDTO;
import com.huang.model.entity.CoachScheduleChange;
import com.huang.vo.AdminCoachScheduleChangeDetailVO;

/**
 * Admin端教练日程审核Service接口
 * @author huang
 * @since 2025-01-24
 */
public interface AdminCoachScheduleChangeService extends IService<CoachScheduleChange> {

    /**
     * 分页查询教练日程变更申请
     */
    IPage<AdminCoachScheduleChangeDetailVO> getCoachScheduleChangePageForAdmin(AdminCoachScheduleQueryDTO query);

    /**
     * 查询日程变更申请详情
     */
    AdminCoachScheduleChangeDetailVO getCoachScheduleChangeDetailForAdmin(Long id);

    /**
     * 审核日程变更申请
     */
    void reviewCoachScheduleChange(String token, CoachScheduleReviewDTO dto);
}