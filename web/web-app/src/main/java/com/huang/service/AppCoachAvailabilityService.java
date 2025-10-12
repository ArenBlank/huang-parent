package com.huang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huang.dto.CoachAvailabilityDTO;
import com.huang.model.entity.CoachAvailability;
import com.huang.vo.CoachAvailabilityVO;

import java.util.List;

/**
 * App端教练可用性Service接口
 * @author huang
 * @since 2025-01-24
 */
public interface AppCoachAvailabilityService extends IService<CoachAvailability> {

    /**
     * 查询教练可用性列表
     */
    List<CoachAvailabilityVO> getCoachAvailabilityList(String token);

    /**
     * 设置教练可用性
     */
    void setCoachAvailability(String token, CoachAvailabilityDTO dto);

    /**
     * 删除教练可用性设置
     */
    void deleteCoachAvailability(String token, Long id);
}