package com.huang.web.app.service;

import com.huang.web.app.dto.coach.CoachResignationApplyDTO;
import com.huang.web.app.vo.coach.CoachResignationVO;

import java.util.List;

/**
 * App端教练离职申请Service接口
 * @author system
 * @since 2025-01-25
 */
public interface CoachResignationService {

    /**
     * 提交教练离职申请
     */
    Long applyResignation(CoachResignationApplyDTO applyDTO);

    /**
     * 获取当前教练的离职申请状态
     */
    CoachResignationVO getCurrentResignationStatus();

    /**
     * 获取离职申请历史记录
     */
    List<CoachResignationVO> getResignationHistory();

    /**
     * 撤销离职申请
     */
    boolean cancelResignation(Long applicationId);

    /**
     * 获取离职申请详情
     */
    CoachResignationVO getResignationDetail(Long applicationId);
}
