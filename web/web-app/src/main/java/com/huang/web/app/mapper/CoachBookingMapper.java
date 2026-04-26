package com.huang.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huang.model.entity.CoachBooking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CoachBookingMapper extends BaseMapper<CoachBooking> {

    @Select("""
            SELECT COUNT(1)
            FROM coach_booking
            WHERE user_id = #{userId}
              AND schedule_id = #{scheduleId}
              AND is_deleted = 0
              AND booking_status IN ('WAIT_PAY', 'PAID', 'COMPLETED')
              AND pay_status IN ('UNPAID', 'PAID')
            """)
    Long countAnyByUserAndSchedule(@Param("userId") Long userId, @Param("scheduleId") Long scheduleId);
}
