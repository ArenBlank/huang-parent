package com.huang.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huang.model.entity.UserLoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.Map;

/**
 * 用户登录日志 Mapper
 */
@Mapper
public interface UserLoginLogMapper extends BaseMapper<UserLoginLog> {

    /**
     * 统计用户在指定日期范围内的登录次数
     */
    @Select("SELECT COUNT(*) FROM user_login_log " +
            "WHERE user_id = #{userId} " +
            "AND login_status = 1 " +
            "AND DATE(login_time) BETWEEN #{startDate} AND #{endDate}")
    Integer countLoginByUserIdAndDateRange(@Param("userId") Long userId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * 统计今日登录用户数
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM user_login_log " +
            "WHERE DATE(login_time) = CURDATE() AND login_status = 1")
    Integer countTodayLoginUsers();

    /**
     * 统计今日登录次数
     */
    @Select("SELECT COUNT(*) FROM user_login_log " +
            "WHERE DATE(login_time) = CURDATE() AND login_status = 1")
    Integer countTodayLogins();
}
