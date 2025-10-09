package com.huang.web.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.model.entity.CoachEvaluation;
import com.huang.web.admin.mapper.CoachEvaluationMapper;
import com.huang.web.admin.service.CoachEvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 教练评价表 服务实现类
 * @author system
 * @since 2025-01-24
 */
@Slf4j
@Service
public class CoachEvaluationServiceImpl extends ServiceImpl<CoachEvaluationMapper, CoachEvaluation> implements CoachEvaluationService {

    @Autowired
    private CoachEvaluationMapper coachEvaluationMapper;

    @Override
    public Page<Map<String, Object>> getEvaluationPage(Map<String, Object> query, long current, long size) {
        Page<CoachEvaluation> page = new Page<>(current, size);
        List<Map<String, Object>> records = coachEvaluationMapper.selectEvaluationPage(page, query);
        
        Page<Map<String, Object>> result = new Page<>(current, size);
        result.setRecords(records);
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        
        return result;
    }

    @Override
    public Map<String, Object> getEvaluationDetail(Long id) {
        Map<String, Object> query = new HashMap<>();
        Page<CoachEvaluation> page = new Page<>(1, 1);
        List<Map<String, Object>> records = coachEvaluationMapper.selectEvaluationPage(page, query);
        
        return records.stream()
                .filter(record -> id.equals(record.get("id")))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean updateEvaluationStatus(Long id, Integer status) {
        LambdaUpdateWrapper<CoachEvaluation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CoachEvaluation::getId, id)
                .set(CoachEvaluation::getStatus, status)
                .set(CoachEvaluation::getUpdateTime, LocalDateTime.now());
        
        return update(updateWrapper);
    }

    @Override
    public Map<String, Object> getCoachEvaluationStats(Long coachId) {
        return coachEvaluationMapper.selectCoachEvaluationStats(coachId);
    }

    @Override
    public List<Map<String, Object>> getCoachRatingDistribution(Long coachId) {
        return coachEvaluationMapper.selectCoachRatingDistribution(coachId);
    }

    @Override
    public List<Map<String, Object>> getUserEvaluationHistory(Long userId) {
        return coachEvaluationMapper.selectUserEvaluationHistory(userId);
    }

    @Override
    public List<Map<String, Object>> getPopularTags(Long coachId) {
        return coachEvaluationMapper.selectPopularTags(coachId);
    }

    @Override
    public List<Map<String, Object>> getCoachRankings(Integer limit) {
        return coachEvaluationMapper.selectCoachRankings(limit != null ? limit : 10);
    }

    @Override
    public boolean deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        
        LambdaUpdateWrapper<CoachEvaluation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(CoachEvaluation::getId, ids)
                .set(CoachEvaluation::getIsDeleted, 1)
                .set(CoachEvaluation::getUpdateTime, LocalDateTime.now());
        
        return update(updateWrapper);
    }

    @Override
    public Map<String, Object> getEvaluationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总评价数
        LambdaQueryWrapper<CoachEvaluation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CoachEvaluation::getIsDeleted, 0)
                .eq(CoachEvaluation::getStatus, 1);
        long totalEvaluations = count(queryWrapper);
        
        // 本月评价数
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CoachEvaluation::getIsDeleted, 0)
                .eq(CoachEvaluation::getStatus, 1)
                .ge(CoachEvaluation::getCreateTime, monthStart);
        long monthEvaluations = count(queryWrapper);
        
        // 今日评价数
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CoachEvaluation::getIsDeleted, 0)
                .eq(CoachEvaluation::getStatus, 1)
                .ge(CoachEvaluation::getCreateTime, todayStart);
        long todayEvaluations = count(queryWrapper);
        
        // 待审核评价数
        queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CoachEvaluation::getIsDeleted, 0)
                .eq(CoachEvaluation::getStatus, 0);
        long pendingEvaluations = count(queryWrapper);
        
        // 平均评分
        List<CoachEvaluation> allEvaluations = list(new LambdaQueryWrapper<CoachEvaluation>()
                .eq(CoachEvaluation::getIsDeleted, 0)
                .eq(CoachEvaluation::getStatus, 1));
        
        double avgRating = 0.0;
        if (!allEvaluations.isEmpty()) {
            avgRating = allEvaluations.stream()
                    .mapToDouble(e -> e.getOverallRating().doubleValue())
                    .average()
                    .orElse(0.0);
        }
        
        stats.put("totalEvaluations", totalEvaluations);
        stats.put("monthEvaluations", monthEvaluations);
        stats.put("todayEvaluations", todayEvaluations);
        stats.put("pendingEvaluations", pendingEvaluations);
        stats.put("avgRating", new BigDecimal(avgRating).setScale(2, BigDecimal.ROUND_HALF_UP));
        
        return stats;
    }

    @Override
    public List<Map<String, Object>> exportEvaluationData(Map<String, Object> query) {
        // 设置一个较大的分页大小来获取所有数据
        Page<CoachEvaluation> page = new Page<>(1, 10000);
        return coachEvaluationMapper.selectEvaluationPage(page, query);
    }
}