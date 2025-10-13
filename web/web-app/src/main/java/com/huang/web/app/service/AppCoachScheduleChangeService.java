package com.huang.web.app.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.huang.web.app.dto.CoachScheduleChangeDTO;
import com.huang.web.app.dto.CoachScheduleQueryDTO;
import com.huang.model.entity.CoachScheduleChange;
import com.huang.web.app.vo.coach.CoachScheduleChangeDetailVO;

/**
 * App端教练日程变更Service接口
 * @author huang
 * @since 2025-01-24
 */
public interface AppCoachScheduleChangeService extends IService<CoachScheduleChange> {

    /**
     * 申请日程变更
     */
    void applyScheduleChange(String token, CoachScheduleChangeDTO dto);

    /**
     * 分页查询教练日程变更申请
     */
    IPage<CoachScheduleChangeDetailVO> getScheduleChangePage(String token, CoachScheduleQueryDTO query);

    /**
     * 查询日程变更申请详情
     */
    CoachScheduleChangeDetailVO getScheduleChangeDetail(String token, Long id);

    /**
     * 取消日程变更申请
     */
    void cancelScheduleChange(String token, Long id);
}