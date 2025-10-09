package com.huang.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.model.entity.CoachConsultation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 教练咨询记录表 Mapper 接口
 * @author system
 * @since 2025-01-24
 */
@Mapper
public interface CoachConsultationMapper extends BaseMapper<CoachConsultation> {

    /**
     * 分页查询咨询记录（带用户和教练信息）
     */
    List<Map<String, Object>> selectConsultationPage(Page<CoachConsultation> page, @Param("query") Map<String, Object> query);

    /**
     * 获取教练的咨询统计信息
     */
    Map<String, Object> selectCoachConsultationStats(@Param("coachId") Long coachId);

    /**
     * 查询用户的咨询历史
     */
    List<Map<String, Object>> selectUserConsultationHistory(@Param("userId") Long userId);
}