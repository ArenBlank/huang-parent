package com.huang.web.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.model.entity.CoachService;
import com.huang.web.admin.mapper.CoachServiceMapper;
import com.huang.web.admin.service.CoachServiceService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 教练服务项目服务实现类
 * @author system
 * @since 2025-01-24
 */
@Service
public class CoachServiceServiceImpl extends ServiceImpl<CoachServiceMapper, CoachService> implements CoachServiceService {

    @Override
    public List<CoachService> listByCoachId(Long coachId) {
        if (coachId == null) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<CoachService>()
                .eq(CoachService::getCoachId, coachId)
                .eq(CoachService::getStatus, 1)
                .orderByAsc(CoachService::getCreateTime));
    }

    @Override
    public List<CoachService> listByServiceType(String serviceType) {
        if (serviceType == null || serviceType.trim().isEmpty()) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<CoachService>()
                .eq(CoachService::getServiceType, serviceType)
                .eq(CoachService::getStatus, 1)
                .orderByAsc(CoachService::getPrice));
    }
}