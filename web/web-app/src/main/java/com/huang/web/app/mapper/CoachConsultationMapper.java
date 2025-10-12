package com.huang.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huang.model.entity.CoachConsultation;
import com.huang.web.app.vo.coach.CoachConsultationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * App端教练咨询Mapper
 * @author system
 * @since 2025-01-25
 */
@Mapper
public interface CoachConsultationMapper extends BaseMapper<CoachConsultation> {

    /**
     * 查询用户的咨询记录
     */
    List<CoachConsultationVO> selectMyConsultations(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 根据ID和用户ID查询咨询详情
     */
    CoachConsultationVO selectConsultationByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 检查时间冲突
     */
    int checkTimeConflict(@Param("coachId") Long coachId, @Param("consultationDate") java.time.LocalDateTime consultationDate, @Param("duration") Integer duration);
}