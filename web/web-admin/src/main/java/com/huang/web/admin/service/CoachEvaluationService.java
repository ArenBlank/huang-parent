package com.huang.web.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.huang.model.entity.CoachEvaluation;

import java.util.List;
import java.util.Map;

/**
 * 教练评价表 服务类
 * @author system
 * @since 2025-01-24
 */
public interface CoachEvaluationService extends IService<CoachEvaluation> {

    /**
     * 分页查询评价记录
     */
    Page<Map<String, Object>> getEvaluationPage(Map<String, Object> query, long current, long size);

    /**
     * 获取评价详情
     */
    Map<String, Object> getEvaluationDetail(Long id);

    /**
     * 审核评价（显示/隐藏）
     */
    boolean updateEvaluationStatus(Long id, Integer status);

    /**
     * 获取教练评价统计
     */
    Map<String, Object> getCoachEvaluationStats(Long coachId);

    /**
     * 获取教练评价分布统计
     */
    List<Map<String, Object>> getCoachRatingDistribution(Long coachId);

    /**
     * 查询用户评价历史
     */
    List<Map<String, Object>> getUserEvaluationHistory(Long userId);

    /**
     * 获取热门评价标签
     */
    List<Map<String, Object>> getPopularTags(Long coachId);

    /**
     * 获取教练评分排行榜
     */
    List<Map<String, Object>> getCoachRankings(Integer limit);

    /**
     * 批量删除评价记录
     */
    boolean deleteBatch(List<Long> ids);

    /**
     * 获取评价数据统计概览
     */
    Map<String, Object> getEvaluationStatistics();

    /**
     * 导出评价数据
     */
    List<Map<String, Object>> exportEvaluationData(Map<String, Object> query);
}