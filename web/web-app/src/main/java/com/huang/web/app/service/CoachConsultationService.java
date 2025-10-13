package com.huang.web.app.service;

import com.huang.web.app.dto.coach.CoachConsultationBookDTO;
import com.huang.web.app.vo.coach.CoachConsultationVO;

import java.util.List;

/**
 * App端教练咨询Service接口
 * @author system
 * @since 2025-01-25
 */
public interface CoachConsultationService {

    /**
     * 预约咨询
     */
    Long bookConsultation(CoachConsultationBookDTO bookDTO);

    /**
     * 查询我的咨询记录
     */
    List<CoachConsultationVO> getMyConsultations(String status);

    /**
     * 获取咨询详情
     */
    CoachConsultationVO getConsultationDetail(Long id);

    /**
     * 取消咨询
     */
    boolean cancelConsultation(Long id);
}
