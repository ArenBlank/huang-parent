package com.huang.web.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huang.model.entity.CoachService;

import java.util.List;

/**
 * 教练服务项目服务接口
 * @author system
 * @since 2025-01-24
 */
public interface CoachServiceService extends IService<CoachService> {

    /**
     * 根据教练ID查询服务项目列表
     * @param coachId 教练ID
     * @return 服务项目列表
     */
    List<CoachService> listByCoachId(Long coachId);

    /**
     * 根据服务类型查询服务项目列表
     * @param serviceType 服务类型
     * @return 服务项目列表
     */
    List<CoachService> listByServiceType(String serviceType);
}