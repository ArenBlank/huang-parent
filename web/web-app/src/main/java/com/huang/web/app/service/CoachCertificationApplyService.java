package com.huang.web.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huang.model.entity.CoachCertificationApply;
import com.huang.web.app.dto.coach.CoachCertificationApplyDTO;
import com.huang.web.app.vo.coach.CoachCertificationApplyVO;

import java.util.List;

/**
 * CoachCertificationApply服务接口
 * @author system
 * @since 2025-01-24
 */
public interface CoachCertificationApplyService extends IService<CoachCertificationApply> {

    /**
     * 提交教练认证申请
     */
    CoachCertificationApplyVO submitApplication(CoachCertificationApplyDTO dto);
    
    /**
     * 获取当前用户的申请状态
     */
    CoachCertificationApplyVO getCurrentUserApplication();
    
    /**
     * 获取用户所有申请记录
     */
    List<CoachCertificationApplyVO> getUserApplicationHistory();
    
    /**
     * 取消申请
     */
    boolean cancelApplication(Long applicationId, String cancelReason);

}
