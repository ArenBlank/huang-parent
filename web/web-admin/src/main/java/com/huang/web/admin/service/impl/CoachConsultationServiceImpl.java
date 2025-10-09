package com.huang.web.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.model.entity.CoachConsultation;
import com.huang.web.admin.mapper.CoachConsultationMapper;
import com.huang.web.admin.service.CoachConsultationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 教练咨询记录表 服务实现类
 * @author system
 * @since 2025-01-24
 */
@Slf4j
@Service
public class CoachConsultationServiceImpl extends ServiceImpl<CoachConsultationMapper, CoachConsultation> implements CoachConsultationService {

    @Autowired
    private CoachConsultationMapper coachConsultationMapper;

    @Override
    public Page<Map<String, Object>> getConsultationPage(Map<String, Object> query, long current, long size) {
        Page<CoachConsultation> page = new Page<>(current, size);
        List<Map<String, Object>> records = coachConsultationMapper.selectConsultationPage(page, query);
        
        Page<Map<String, Object>> result = new Page<>(current, size);
        result.setRecords(records);
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        
        return result;
    }

    @Override
    public Map<String, Object> getConsultationDetail(Long id) {
        // 直接通过查询获取详情
        Map<String, Object> query = new HashMap<>();
        Page<CoachConsultation> page = new Page<>(1, 1);
        List<Map<String, Object>> records = coachConsultationMapper.selectConsultationPage(page, query);
        
        return records.stream()
                .filter(record -> id.equals(record.get("id")))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean updateConsultationStatus(Long id, String status) {
        LambdaUpdateWrapper<CoachConsultation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CoachConsultation::getId, id)
                .set(CoachConsultation::getStatus, status)
                .set(CoachConsultation::getUpdateTime, LocalDateTime.now());
        
        // 如果状态是已完成，设置完成时间为当前时间
        if ("completed".equals(status)) {
            // 这里需要考虑到数据库字段，暂时通过SQL更新
        }
        
        return update(updateWrapper);
    }

    @Override
    public boolean addCoachAdvice(Long id, String coachAdvice) {
        LambdaUpdateWrapper<CoachConsultation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CoachConsultation::getId, id)
                .set(CoachConsultation::getCoachAdvice, coachAdvice)
                .set(CoachConsultation::getUpdateTime, LocalDateTime.now());
        
        return update(updateWrapper);
    }

    @Override
    public Map<String, Object> getCoachConsultationStats(Long coachId) {
        return coachConsultationMapper.selectCoachConsultationStats(coachId);
    }

    @Override
    public List<Map<String, Object>> getUserConsultationHistory(Long userId) {
        return coachConsultationMapper.selectUserConsultationHistory(userId);
    }

    @Override
    public boolean deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        
        LambdaUpdateWrapper<CoachConsultation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(CoachConsultation::getId, ids)
                .set(CoachConsultation::getIsDeleted, 1)
                .set(CoachConsultation::getUpdateTime, LocalDateTime.now());
        
        return update(updateWrapper);
    }

    @Override
    public Map<String, Object> getConsultationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总咨询数
        LambdaQueryWrapper<CoachConsultation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CoachConsultation::getIsDeleted, 0);
        long totalConsultations = count(queryWrapper);
        
        // 本月咨询数
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CoachConsultation::getIsDeleted, 0)
                .ge(CoachConsultation::getConsultationDate, monthStart);
        long monthConsultations = count(queryWrapper);
        
        // 今日咨询数
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CoachConsultation::getIsDeleted, 0)
                .ge(CoachConsultation::getConsultationDate, todayStart);
        long todayConsultations = count(queryWrapper);
        
        // 待处理咨询数
        queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CoachConsultation::getIsDeleted, 0)
                .eq(CoachConsultation::getStatus, "scheduled");
        long pendingConsultations = count(queryWrapper);
        
        stats.put("totalConsultations", totalConsultations);
        stats.put("monthConsultations", monthConsultations);
        stats.put("todayConsultations", todayConsultations);
        stats.put("pendingConsultations", pendingConsultations);
        
        return stats;
    }
}