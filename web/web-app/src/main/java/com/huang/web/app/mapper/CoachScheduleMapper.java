package com.huang.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huang.model.entity.CoachSchedule;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CoachScheduleMapper extends BaseMapper<CoachSchedule> {

    @Update("""
            UPDATE coach_schedule
            SET booked_count = booked_count + 1
            WHERE id = #{scheduleId}
              AND status = 1
              AND is_deleted = 0
              AND booked_count < capacity
            """)
    int reserveSlot(@Param("scheduleId") Long scheduleId);
}
