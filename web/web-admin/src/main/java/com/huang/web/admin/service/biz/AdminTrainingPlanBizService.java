package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huang.common.constant.RedisConstant;
import com.huang.common.redis.MultiLevelCacheSupport;
import com.huang.model.entity.TrainingPlan;
import com.huang.model.entity.TrainingPlanItem;
import com.huang.model.entity.VideoAsset;
import com.huang.web.admin.dto.plan.TrainingPlanItemUpsertDTO;
import com.huang.web.admin.dto.plan.TrainingPlanUpsertDTO;
import com.huang.web.admin.mapper.TrainingPlanItemMapper;
import com.huang.web.admin.mapper.TrainingPlanMapper;
import com.huang.web.admin.mapper.TrainingPlanSubscribeMapper;
import com.huang.web.admin.mapper.TrainingRecordMapper;
import com.huang.web.admin.mapper.VideoAssetMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminTrainingPlanBizService {

    private final TrainingPlanMapper trainingPlanMapper;
    private final TrainingPlanItemMapper trainingPlanItemMapper;
    private final TrainingPlanSubscribeMapper trainingPlanSubscribeMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final VideoAssetMapper videoAssetMapper;
    private final MultiLevelCacheSupport multiLevelCacheSupport;

    public AdminTrainingPlanBizService(TrainingPlanMapper trainingPlanMapper,
                                       TrainingPlanItemMapper trainingPlanItemMapper,
                                       TrainingPlanSubscribeMapper trainingPlanSubscribeMapper,
                                       TrainingRecordMapper trainingRecordMapper,
                                       VideoAssetMapper videoAssetMapper,
                                       MultiLevelCacheSupport multiLevelCacheSupport) {
        this.trainingPlanMapper = trainingPlanMapper;
        this.trainingPlanItemMapper = trainingPlanItemMapper;
        this.trainingPlanSubscribeMapper = trainingPlanSubscribeMapper;
        this.trainingRecordMapper = trainingRecordMapper;
        this.videoAssetMapper = videoAssetMapper;
        this.multiLevelCacheSupport = multiLevelCacheSupport;
    }

    public List<Map<String, Object>> listPlans(Integer status) {
        List<TrainingPlan> plans = trainingPlanMapper.selectList(
                Wrappers.<TrainingPlan>lambdaQuery()
                        .isNull(TrainingPlan::getOwnerUserId)
                        .eq(status != null, TrainingPlan::getStatus, status)
                        .orderByDesc(TrainingPlan::getId)
        );
        if (plans.isEmpty()) {
            return List.of();
        }

        List<Long> planIds = plans.stream().map(TrainingPlan::getId).toList();

        Map<Long, Long> itemCountMap = trainingPlanItemMapper.selectList(
                        Wrappers.<TrainingPlanItem>lambdaQuery()
                                .in(TrainingPlanItem::getPlanId, planIds)
                ).stream()
                .collect(Collectors.groupingBy(TrainingPlanItem::getPlanId, Collectors.counting()));

        Map<Long, Long> activeSubscribeCountMap = trainingPlanSubscribeMapper.selectList(
                        Wrappers.<com.huang.model.entity.TrainingPlanSubscribe>lambdaQuery()
                                .in(com.huang.model.entity.TrainingPlanSubscribe::getPlanId, planIds)
                                .eq(com.huang.model.entity.TrainingPlanSubscribe::getStatus, 1)
                ).stream()
                .collect(Collectors.groupingBy(com.huang.model.entity.TrainingPlanSubscribe::getPlanId, Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>(plans.size());
        for (TrainingPlan plan : plans) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", plan.getId());
            row.put("title", plan.getTitle());
            row.put("goal", plan.getGoal());
            row.put("level", plan.getLevel());
            row.put("durationWeeks", plan.getDurationWeeks());
            row.put("coverUrl", plan.getCoverUrl());
            row.put("status", plan.getStatus());
            row.put("itemCount", itemCountMap.getOrDefault(plan.getId(), 0L));
            row.put("activeSubscribeCount", activeSubscribeCountMap.getOrDefault(plan.getId(), 0L));
            result.add(row);
        }
        return result;
    }

    public Map<String, Object> getPlanDetail(Long planId) {
        TrainingPlan plan = trainingPlanMapper.selectById(planId);
        if (plan == null || plan.getOwnerUserId() != null) {
            return null;
        }

        List<TrainingPlanItem> items = trainingPlanItemMapper.selectList(
                Wrappers.<TrainingPlanItem>lambdaQuery()
                        .eq(TrainingPlanItem::getPlanId, planId)
                        .orderByAsc(TrainingPlanItem::getDayIndex, TrainingPlanItem::getSort, TrainingPlanItem::getId)
        );

        Set<Long> videoIds = items.stream()
                .map(TrainingPlanItem::getVideoId)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toSet());
        Map<Long, VideoAsset> videoMap = videoIds.isEmpty()
                ? Map.of()
                : videoAssetMapper.selectBatchIds(videoIds).stream()
                        .collect(Collectors.toMap(VideoAsset::getId, item -> item));

        List<Map<String, Object>> itemViews = new ArrayList<>(items.size());
        for (TrainingPlanItem item : items) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", item.getId());
            row.put("planId", item.getPlanId());
            row.put("dayIndex", item.getDayIndex());
            row.put("actionName", item.getActionName());
            row.put("sets", item.getSets());
            row.put("reps", item.getReps());
            row.put("durationMin", item.getDurationMin());
            row.put("restSec", item.getRestSec());
            row.put("videoId", item.getVideoId());
            row.put("sort", item.getSort());
            VideoAsset video = item.getVideoId() == null ? null : videoMap.get(item.getVideoId());
            row.put("videoTitle", video == null ? null : video.getTitle());
            itemViews.add(row);
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("plan", plan);
        detail.put("items", itemViews);
        detail.put("activeSubscribeCount", trainingPlanSubscribeMapper.selectCount(
                Wrappers.<com.huang.model.entity.TrainingPlanSubscribe>lambdaQuery()
                        .eq(com.huang.model.entity.TrainingPlanSubscribe::getPlanId, planId)
                        .eq(com.huang.model.entity.TrainingPlanSubscribe::getStatus, 1)
        ));
        detail.put("recordCount", trainingRecordMapper.selectCount(
                new LambdaQueryWrapper<com.huang.model.entity.TrainingRecord>()
                        .eq(com.huang.model.entity.TrainingRecord::getPlanId, planId)
        ));
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createPlan(TrainingPlanUpsertDTO dto) {
        TrainingPlan plan = new TrainingPlan();
        fillPlan(plan, dto);
        trainingPlanMapper.insert(plan);
        clearPlanListCache();
        clearPlanDetailCache(plan.getId());
        return plan.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updatePlan(Long id, TrainingPlanUpsertDTO dto) {
        TrainingPlan exists = trainingPlanMapper.selectById(id);
        if (exists == null || exists.getOwnerUserId() != null) {
            return false;
        }
        fillPlan(exists, dto);
        boolean updated = trainingPlanMapper.updateById(exists) > 0;
        if (updated) {
            clearPlanListCache();
            clearPlanDetailCache(id);
        }
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deletePlan(Long id) {
        TrainingPlan exists = trainingPlanMapper.selectById(id);
        if (exists == null || exists.getOwnerUserId() != null) {
            return false;
        }
        long subscribeCount = trainingPlanSubscribeMapper.selectCount(
                Wrappers.<com.huang.model.entity.TrainingPlanSubscribe>lambdaQuery()
                        .eq(com.huang.model.entity.TrainingPlanSubscribe::getPlanId, id)
        );
        if (subscribeCount > 0) {
            throw new IllegalStateException("宸叉湁鐢ㄦ埛璁㈤槄璇ヨ鍒掞紝涓嶈兘鍒犻櫎");
        }
        long recordCount = trainingRecordMapper.selectCount(
                new LambdaQueryWrapper<com.huang.model.entity.TrainingRecord>()
                        .eq(com.huang.model.entity.TrainingRecord::getPlanId, id)
        );
        if (recordCount > 0) {
            throw new IllegalStateException("宸叉湁璁粌鎵撳崱璁板綍鍏宠仈璇ヨ鍒掞紝涓嶈兘鍒犻櫎");
        }
        trainingPlanItemMapper.delete(
                Wrappers.<TrainingPlanItem>lambdaQuery().eq(TrainingPlanItem::getPlanId, id)
        );
        boolean deleted = trainingPlanMapper.deleteById(id) > 0;
        if (deleted) {
            clearPlanListCache();
            clearPlanDetailCache(id);
        }
        return deleted;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createPlanItem(Long planId, TrainingPlanItemUpsertDTO dto) {
        TrainingPlan plan = trainingPlanMapper.selectById(planId);
        if (plan == null || plan.getOwnerUserId() != null) {
            return null;
        }
        validateVideo(dto.getVideoId());
        TrainingPlanItem item = new TrainingPlanItem();
        item.setPlanId(planId);
        fillPlanItem(item, dto);
        trainingPlanItemMapper.insert(item);
        clearPlanListCache();
        clearPlanDetailCache(planId);
        return item.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updatePlanItem(Long itemId, TrainingPlanItemUpsertDTO dto) {
        TrainingPlanItem exists = trainingPlanItemMapper.selectById(itemId);
        if (exists == null) {
            return false;
        }
        TrainingPlan plan = trainingPlanMapper.selectById(exists.getPlanId());
        if (plan == null || plan.getOwnerUserId() != null) {
            return false;
        }
        validateVideo(dto.getVideoId());
        Long planId = exists.getPlanId();
        fillPlanItem(exists, dto);
        boolean updated = trainingPlanItemMapper.updateById(exists) > 0;
        if (updated) {
            clearPlanListCache();
            clearPlanDetailCache(planId);
        }
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deletePlanItem(Long itemId) {
        TrainingPlanItem exists = trainingPlanItemMapper.selectById(itemId);
        if (exists == null) {
            return false;
        }
        TrainingPlan plan = trainingPlanMapper.selectById(exists.getPlanId());
        if (plan == null || plan.getOwnerUserId() != null) {
            return false;
        }
        long recordCount = trainingRecordMapper.selectCount(
                new LambdaQueryWrapper<com.huang.model.entity.TrainingRecord>()
                        .eq(com.huang.model.entity.TrainingRecord::getPlanItemId, itemId)
        );
        if (recordCount > 0) {
            throw new IllegalStateException("宸叉湁璁粌鎵撳崱璁板綍鍏宠仈璇ヨ鍒掗」锛屼笉鑳藉垹闄?");
        }
        boolean deleted = trainingPlanItemMapper.deleteById(itemId) > 0;
        if (deleted) {
            clearPlanListCache();
            clearPlanDetailCache(exists.getPlanId());
        }
        return deleted;
    }

    private void fillPlan(TrainingPlan plan, TrainingPlanUpsertDTO dto) {
        plan.setTitle(dto.getTitle());
        plan.setGoal(dto.getGoal());
        plan.setLevel(dto.getLevel());
        plan.setDurationWeeks(dto.getDurationWeeks());
        plan.setCoverUrl(dto.getCoverUrl());
        plan.setStatus(dto.getStatus());
        plan.setOwnerUserId(null);
    }

    private void fillPlanItem(TrainingPlanItem item, TrainingPlanItemUpsertDTO dto) {
        item.setDayIndex(dto.getDayIndex());
        item.setActionName(dto.getActionName());
        item.setSets(dto.getSets() == null ? 0 : dto.getSets());
        item.setReps(dto.getReps() == null ? 0 : dto.getReps());
        item.setDurationMin(dto.getDurationMin() == null ? 0 : dto.getDurationMin());
        item.setRestSec(dto.getRestSec() == null ? 0 : dto.getRestSec());
        item.setVideoId(dto.getVideoId());
        item.setSort(dto.getSort() == null ? 0 : dto.getSort());
    }

    private void validateVideo(Long videoId) {
        if (videoId == null || videoId <= 0) {
            return;
        }
        VideoAsset videoAsset = videoAssetMapper.selectById(videoId);
        if (videoAsset == null || videoAsset.getStatus() == null || videoAsset.getStatus() != 1) {
            throw new IllegalStateException("缁戝畾鐨勮棰戜笉瀛樺湪鎴栨湭鍚敤");
        }
    }

    private void clearPlanListCache() {
        multiLevelCacheSupport.sharedEvict(RedisConstant.APP_PLAN_LIST_ACTIVE_KEY);
    }

    private void clearPlanDetailCache(Long planId) {
        if (planId != null) {
            multiLevelCacheSupport.sharedEvict(RedisConstant.appPlanDetailStaticKey(planId));
        }
    }
}
