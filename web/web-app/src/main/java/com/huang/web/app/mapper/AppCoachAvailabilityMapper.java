package com.huang.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huang.model.entity.CoachAvailability;
import com.huang.web.app.vo.coach.CoachAvailabilityVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * App端教练可用性Mapper
 * @author huang
 * @since 2025-01-24
 */
@Mapper
public interface AppCoachAvailabilityMapper extends BaseMapper<CoachAvailability> {

    /**
     * 查询教练可用性列表
     */
    List<CoachAvailabilityVO> selectCoachAvailabilityList(@Param("coachId") Long coachId);

    /**
     * 根据教练ID和星期几查询可用性
     */
    CoachAvailability selectByCoachIdAndDayOfWeek(@Param("coachId") Long coachId, @Param("dayOfWeek") Integer dayOfWeek);
}