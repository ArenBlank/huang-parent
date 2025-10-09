package com.huang.web.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.huang.model.entity.CoachConsultation;

import java.util.List;
import java.util.Map;

/**
 * 教练咨询记录表 服务类
 * @author system
 * @since 2025-01-24
 */
public interface CoachConsultationService extends IService<CoachConsultation> {

    /**
     * 分页查询咨询记录
     */
    Page<Map<String, Object>> getConsultationPage(Map<String, Object> query, long current, long size);

    /**
     * 获取咨询详情
     */
    Map<String, Object> getConsultationDetail(Long id);

    /**
     * 更新咨询状态
     */
    boolean updateConsultationStatus(Long id, String status);

    /**
     * 添加教练建议
     */
    boolean addCoachAdvice(Long id, String coachAdvice);

    /**
     * 获取教练咨询统计
     */
    Map<String, Object> getCoachConsultationStats(Long coachId);

    /**
     * 查询用户咨询历史
     */
    List<Map<String, Object>> getUserConsultationHistory(Long userId);

    /**
     * 批量删除咨询记录
     */
    boolean deleteBatch(List<Long> ids);

    /**
     * 获取咨询数据统计
     */
    Map<String, Object> getConsultationStatistics();
}