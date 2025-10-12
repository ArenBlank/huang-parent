package com.huang.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huang.model.entity.CoachResignationApply;
import com.huang.web.app.vo.coach.CoachResignationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * App端教练离职申请Mapper
 * @author system
 * @since 2025-01-25
 */
@Mapper
public interface CoachResignationApplyMapper extends BaseMapper<CoachResignationApply> {

    /**
     * 根据教练ID查询当前待审核的离职申请
     */
    CoachResignationVO selectPendingByCoachId(@Param("coachId") Long coachId);

    /**
     * 根据教练ID查询离职申请历史
     */
    List<CoachResignationVO> selectHistoryByCoachId(@Param("coachId") Long coachId);

    /**
     * 根据申请ID和教练ID查询离职申请详情
     */
    CoachResignationVO selectByIdAndCoachId(@Param("id") Long id, @Param("coachId") Long coachId);
}
