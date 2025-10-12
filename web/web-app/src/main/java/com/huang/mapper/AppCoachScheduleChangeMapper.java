package com.huang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.dto.CoachScheduleQueryDTO;
import com.huang.model.entity.CoachScheduleChange;
import com.huang.vo.CoachScheduleChangeDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * App端教练日程变更Mapper
 * @author huang
 * @since 2025-01-24
 */
@Mapper
public interface AppCoachScheduleChangeMapper extends BaseMapper<CoachScheduleChange> {

    /**
     * 分页查询教练日程变更申请
     */
    IPage<CoachScheduleChangeDetailVO> selectCoachScheduleChangePage(
            Page<?> page, 
            @Param("coachId") Long coachId, 
            @Param("query") CoachScheduleQueryDTO query
    );

    /**
     * 根据ID查询教练日程变更申请详情
     */
    CoachScheduleChangeDetailVO selectCoachScheduleChangeDetail(@Param("id") Long id, @Param("coachId") Long coachId);
}