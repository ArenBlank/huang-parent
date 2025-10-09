package com.huang.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.model.entity.CoachEvaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 教练评价表 Mapper 接口
 * @author system
 * @since 2025-01-24
 */
@Mapper
public interface CoachEvaluationMapper extends BaseMapper<CoachEvaluation> {

    /**
     * 分页查询教练评价（带用户和教练信息）
     */
    List<Map<String, Object>> selectEvaluationPage(Page<CoachEvaluation> page, @Param("query") Map<String, Object> query);

    /**
     * 获取教练的评价统计信息
     */
    Map<String, Object> selectCoachEvaluationStats(@Param("coachId") Long coachId);

    /**
     * 获取教练的评价分布统计
     */
    List<Map<String, Object>> selectCoachRatingDistribution(@Param("coachId") Long coachId);

    /**
     * 查询用户的评价历史
     */
    List<Map<String, Object>> selectUserEvaluationHistory(@Param("userId") Long userId);

    /**
     * 获取热门评价标签统计
     */
    List<Map<String, Object>> selectPopularTags(@Param("coachId") Long coachId);

    /**
     * 获取教练综合评分排行榜
     */
    List<Map<String, Object>> selectCoachRankings(@Param("limit") int limit);
}