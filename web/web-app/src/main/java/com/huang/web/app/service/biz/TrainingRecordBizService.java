package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.model.entity.TrainingPlanSubscribe;
import com.huang.model.entity.TrainingRecord;
import com.huang.web.app.dto.record.TrainingCheckinDTO;
import com.huang.web.app.mapper.TrainingPlanSubscribeMapper;
import com.huang.web.app.mapper.TrainingRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrainingRecordBizService {

    private final TrainingRecordMapper trainingRecordMapper;
    private final TrainingPlanSubscribeMapper trainingPlanSubscribeMapper;

    public TrainingRecordBizService(TrainingRecordMapper trainingRecordMapper,
                                    TrainingPlanSubscribeMapper trainingPlanSubscribeMapper) {
        this.trainingRecordMapper = trainingRecordMapper;
        this.trainingPlanSubscribeMapper = trainingPlanSubscribeMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean checkin(Long userId, TrainingCheckinDTO dto) {
        TrainingPlanSubscribe subscribe = trainingPlanSubscribeMapper.selectOne(
                new LambdaQueryWrapper<TrainingPlanSubscribe>()
                        .eq(TrainingPlanSubscribe::getUserId, userId)
                        .eq(TrainingPlanSubscribe::getPlanId, dto.getPlanId())
                        .eq(TrainingPlanSubscribe::getStatus, 1)
                        .last("LIMIT 1")
        );
        if (subscribe == null) {
            return false;
        }

        // 工程亮点：只允许“已订阅且激活”的计划打卡，避免非法写入与脏统计。
        TrainingRecord record = new TrainingRecord();
        record.setUserId(userId);
        record.setPlanId(dto.getPlanId());
        record.setPlanItemId(dto.getPlanItemId());
        record.setRecordDate(dto.getRecordDate());
        record.setDurationMin(dto.getDurationMin());
        record.setCalories(dto.getCalories());
        record.setFeeling(dto.getFeeling());
        record.setRecordImages(dto.getRecordImages());
        return trainingRecordMapper.insert(record) > 0;
    }

    public List<TrainingRecord> listByUser(Long userId, Integer limit) {
        int size = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
        return trainingRecordMapper.selectList(
                new LambdaQueryWrapper<TrainingRecord>()
                        .eq(TrainingRecord::getUserId, userId)
                        .orderByDesc(TrainingRecord::getRecordDate, TrainingRecord::getId)
                        .last("LIMIT " + size)
        );
    }

    public Map<String, Object> weeklyStat(Long userId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        List<TrainingRecord> records = trainingRecordMapper.selectList(
                new LambdaQueryWrapper<TrainingRecord>()
                        .eq(TrainingRecord::getUserId, userId)
                        .ge(TrainingRecord::getRecordDate, start)
                        .le(TrainingRecord::getRecordDate, end)
        );
        // 工程亮点：聚合在服务层完成，接口返回统计结果，前端无需重复计算。
        int totalDuration = records.stream().map(TrainingRecord::getDurationMin).filter(v -> v != null).mapToInt(Integer::intValue).sum();
        int totalCalories = records.stream().map(TrainingRecord::getCalories).filter(v -> v != null).mapToInt(Integer::intValue).sum();

        Map<String, Object> stat = new HashMap<>();
        stat.put("startDate", start);
        stat.put("endDate", end);
        stat.put("days", 7);
        stat.put("checkinCount", records.size());
        stat.put("totalDurationMin", totalDuration);
        stat.put("totalCalories", totalCalories);
        return stat;
    }
}
