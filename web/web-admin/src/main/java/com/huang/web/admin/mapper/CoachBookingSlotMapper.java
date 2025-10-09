package com.huang.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.model.entity.CoachBookingSlot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 教练预约时段表 Mapper 接口
 * @author system
 * @since 2025-01-24
 */
@Mapper
public interface CoachBookingSlotMapper extends BaseMapper<CoachBookingSlot> {

    /**
     * 分页查询预约时段（带教练和用户信息）
     */
    List<Map<String, Object>> selectBookingSlotPage(Page<CoachBookingSlot> page, @Param("query") Map<String, Object> query);

    /**
     * 获取教练的预约统计信息
     */
    Map<String, Object> selectCoachBookingStats(@Param("coachId") Long coachId);

    /**
     * 查询教练在指定日期范围的预约时段
     */
    List<Map<String, Object>> selectCoachAvailableSlots(@Param("coachId") Long coachId, 
                                                        @Param("startDate") LocalDate startDate, 
                                                        @Param("endDate") LocalDate endDate);

    /**
     * 查询用户的预约历史
     */
    List<Map<String, Object>> selectUserBookingHistory(@Param("userId") Long userId);

    /**
     * 查询时段冲突
     */
    List<CoachBookingSlot> selectConflictSlots(@Param("coachId") Long coachId, 
                                              @Param("bookingDate") LocalDate bookingDate,
                                              @Param("startTime") String startTime, 
                                              @Param("endTime") String endTime,
                                              @Param("excludeId") Long excludeId);

    /**
     * 获取预约数据统计
     */
    Map<String, Object> selectBookingStatistics(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}