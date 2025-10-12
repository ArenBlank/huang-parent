package com.huang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.dto.AdminCoachScheduleQueryDTO;
import com.huang.model.entity.CoachScheduleChange;
import com.huang.vo.AdminCoachScheduleChangeDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Admin端教练日程变更Mapper
 * @author huang
 * @since 2025-01-24
 */
@Mapper
public interface AdminCoachScheduleChangeMapper extends BaseMapper<CoachScheduleChange> {

    /**
     * 分页查询教练日程变更申请
     */
    IPage<AdminCoachScheduleChangeDetailVO> selectCoachScheduleChangePageForAdmin(
            Page<?> page,
            @Param("query") AdminCoachScheduleQueryDTO query
    );

    /**
     * 根据ID查询教练日程变更申请详情
     */
    AdminCoachScheduleChangeDetailVO selectCoachScheduleChangeDetailForAdmin(@Param("id") Long id);
}