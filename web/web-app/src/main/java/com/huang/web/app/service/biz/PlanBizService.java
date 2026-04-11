package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.huang.common.constant.RedisConstant;
import com.huang.common.redis.MultiLevelCacheSupport;
import com.huang.model.entity.TrainingPlan;
import com.huang.model.entity.TrainingPlanItem;
import com.huang.model.entity.TrainingPlanSubscribe;
import com.huang.model.entity.VideoAsset;
import com.huang.web.app.dto.plan.PlanSubscribeDTO;
import com.huang.web.app.mapper.TrainingPlanItemMapper;
import com.huang.web.app.mapper.TrainingPlanMapper;
import com.huang.web.app.mapper.TrainingPlanSubscribeMapper;
import com.huang.web.app.mapper.VideoAssetMapper;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class PlanBizService {

    private final TrainingPlanMapper trainingPlanMapper;
    private final TrainingPlanItemMapper trainingPlanItemMapper;
    private final TrainingPlanSubscribeMapper trainingPlanSubscribeMapper;
    private final VideoAssetMapper videoAssetMapper;
    private final MinioClient minioClient;
    private final MultiLevelCacheSupport multiLevelCacheSupport;

    @Value("${minio.public-endpoint:http://files.localhost}")
    private String minioPublicEndpoint;

    @Value("${minio.bucket-name:fitness-platform}")
    private String minioBucketName;

    public PlanBizService(TrainingPlanMapper trainingPlanMapper,
                          TrainingPlanItemMapper trainingPlanItemMapper,
                          TrainingPlanSubscribeMapper trainingPlanSubscribeMapper,
                          VideoAssetMapper videoAssetMapper,
                          ObjectProvider<MinioClient> minioClientProvider,
                          MultiLevelCacheSupport multiLevelCacheSupport) {
        this.trainingPlanMapper = trainingPlanMapper;
        this.trainingPlanItemMapper = trainingPlanItemMapper;
        this.trainingPlanSubscribeMapper = trainingPlanSubscribeMapper;
        this.videoAssetMapper = videoAssetMapper;
        this.minioClient = minioClientProvider.getIfAvailable();
        this.multiLevelCacheSupport = multiLevelCacheSupport;
    }

    public List<Map<String, Object>> listActivePlans(Long userId) {
        List<PlanListStaticView> staticRows = loadActivePlanStatics();
        if (staticRows.isEmpty()) {
            return List.of();
        }

        Map<Long, TrainingPlanSubscribe> subscribeMap = loadUserSubscribeMap(userId);
        return staticRows.stream().map(plan -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", plan.id());
            row.put("title", plan.title());
            row.put("goal", plan.goal());
            row.put("level", plan.level());
            row.put("durationWeeks", plan.durationWeeks());
            row.put("coverUrl", plan.coverUrl());
            row.put("status", plan.status());
            TrainingPlanSubscribe subscribe = subscribeMap.get(plan.id());
            row.put("subscribed", subscribe != null && subscribe.getStatus() != null && subscribe.getStatus() == 1);
            row.put("startDate", subscribe == null ? null : subscribe.getStartDate());
            return row;
        }).toList();
    }

    public Map<String, Object> getPlanDetail(Long planId, Long userId) {
        if (planId == null || planId <= 0) {
            return null;
        }

        PlanDetailStaticCache staticDetail = getStaticPlanDetail(planId);
        if (staticDetail == null) {
            return null;
        }

        TrainingPlanSubscribe subscribe = userId == null ? null : loadPlanSubscribe(userId, planId);

        Map<String, Object> detail = new HashMap<>();
        detail.put("plan", staticDetail.plan());
        detail.put("items", buildDetailItems(staticDetail.items()));
        if (subscribe == null) {
            detail.put("subscription", null);
        } else {
            Map<String, Object> subscription = new HashMap<>();
            subscription.put("planId", subscribe.getPlanId());
            subscription.put("startDate", subscribe.getStartDate());
            subscription.put("status", subscribe.getStatus());
            detail.put("subscription", subscription);
        }
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean subscribe(Long userId, PlanSubscribeDTO dto) {
        TrainingPlan plan = trainingPlanMapper.selectById(dto.getPlanId());
        if (plan == null || plan.getStatus() == null || plan.getStatus() != 1) {
            return false;
        }

        TrainingPlanSubscribe existed = loadPlanSubscribe(userId, dto.getPlanId());
        if (existed != null) {
            existed.setStartDate(dto.getStartDate());
            existed.setStatus(1);
            return trainingPlanSubscribeMapper.updateById(existed) > 0;
        }

        TrainingPlanSubscribe subscribe = new TrainingPlanSubscribe();
        subscribe.setPlanId(dto.getPlanId());
        subscribe.setUserId(userId);
        subscribe.setStartDate(dto.getStartDate());
        subscribe.setStatus(1);
        return trainingPlanSubscribeMapper.insert(subscribe) > 0;
    }

    private List<PlanListStaticView> loadActivePlanStatics() {
        var cached = multiLevelCacheSupport.getJson(
                RedisConstant.APP_PLAN_LIST_ACTIVE_KEY,
                new TypeReference<List<PlanListStaticView>>() {}
        );
        if (cached.found()) {
            return cached.nullValue() ? List.of() : cached.value();
        }

        List<TrainingPlan> plans = trainingPlanMapper.selectList(
                new LambdaQueryWrapper<TrainingPlan>()
                        .eq(TrainingPlan::getStatus, 1)
                        .orderByDesc(TrainingPlan::getId)
        );
        List<PlanListStaticView> rows = plans.stream()
                .map(plan -> new PlanListStaticView(
                        plan.getId(),
                        plan.getTitle(),
                        plan.getGoal(),
                        plan.getLevel(),
                        plan.getDurationWeeks(),
                        plan.getCoverUrl(),
                        plan.getStatus()
                ))
                .toList();
        multiLevelCacheSupport.setJson(
                RedisConstant.APP_PLAN_LIST_ACTIVE_KEY,
                rows,
                multiLevelCacheSupport.ttlWithJitter(RedisConstant.APP_PLAN_LIST_TTL_SEC, RedisConstant.JITTER_SHORT_SEC)
        );
        return rows;
    }

    private PlanDetailStaticCache getStaticPlanDetail(Long planId) {
        String cacheKey = RedisConstant.appPlanDetailStaticKey(planId);
        var cached = multiLevelCacheSupport.getJson(cacheKey, new TypeReference<PlanDetailStaticCache>() {});
        if (cached.found()) {
            return cached.nullValue() ? null : cached.value();
        }

        String lockKey = RedisConstant.appPlanDetailLockKey(planId);
        String lockToken = multiLevelCacheSupport.newLockToken();
        boolean locked = multiLevelCacheSupport.tryLock(lockKey, lockToken, RedisConstant.CACHE_LOCK_TTL_SEC);
        if (!locked) {
            PlanDetailStaticCache retried = waitForStaticPlanCache(cacheKey);
            if (retried != null) {
                return retried;
            }
            return loadStaticPlanDetailFromDb(planId);
        }

        try {
            var secondRead = multiLevelCacheSupport.getJson(cacheKey, new TypeReference<PlanDetailStaticCache>() {});
            if (secondRead.found()) {
                return secondRead.nullValue() ? null : secondRead.value();
            }

            PlanDetailStaticCache loaded = loadStaticPlanDetailFromDb(planId);
            if (loaded == null) {
                multiLevelCacheSupport.cacheNull(cacheKey, RedisConstant.CACHE_NULL_TTL_SEC);
                return null;
            }
            multiLevelCacheSupport.setJson(
                    cacheKey,
                    loaded,
                    multiLevelCacheSupport.ttlWithJitter(RedisConstant.APP_PLAN_DETAIL_TTL_SEC, RedisConstant.JITTER_SHORT_SEC)
            );
            return loaded;
        } finally {
            multiLevelCacheSupport.unlock(lockKey, lockToken);
        }
    }

    private PlanDetailStaticCache waitForStaticPlanCache(String cacheKey) {
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(60L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            var cached = multiLevelCacheSupport.getJson(cacheKey, new TypeReference<PlanDetailStaticCache>() {});
            if (!cached.found()) {
                continue;
            }
            return cached.nullValue() ? null : cached.value();
        }
        return null;
    }

    private PlanDetailStaticCache loadStaticPlanDetailFromDb(Long planId) {
        TrainingPlan plan = trainingPlanMapper.selectById(planId);
        if (plan == null || plan.getStatus() == null || plan.getStatus() != 1) {
            return null;
        }

        List<TrainingPlanItem> items = trainingPlanItemMapper.selectList(
                new LambdaQueryWrapper<TrainingPlanItem>()
                        .eq(TrainingPlanItem::getPlanId, planId)
                        .orderByAsc(TrainingPlanItem::getDayIndex, TrainingPlanItem::getSort)
        );

        Set<Long> videoIds = items.stream()
                .map(TrainingPlanItem::getVideoId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        Map<Long, VideoAsset> videoMap = new HashMap<>();
        if (!videoIds.isEmpty()) {
            videoMap = videoAssetMapper.selectBatchIds(videoIds).stream()
                    .collect(Collectors.toMap(VideoAsset::getId, v -> v));
        }

        List<PlanItemStaticView> itemViews = new ArrayList<>(items.size());
        for (TrainingPlanItem item : items) {
            VideoAsset video = videoMap.get(item.getVideoId());
            PlanVideoStaticView videoView = video == null ? null : new PlanVideoStaticView(
                    video.getId(),
                    video.getTitle(),
                    video.getDurationSec(),
                    video.getTags(),
                    video.getMinioPath(),
                    video.getSourceUrl()
            );
            itemViews.add(new PlanItemStaticView(
                    item.getId(),
                    item.getPlanId(),
                    item.getDayIndex(),
                    item.getActionName(),
                    item.getSets(),
                    item.getReps(),
                    item.getDurationMin(),
                    item.getRestSec(),
                    item.getSort(),
                    videoView
            ));
        }
        return new PlanDetailStaticCache(plan, itemViews);
    }

    private List<Map<String, Object>> buildDetailItems(List<PlanItemStaticView> staticItems) {
        List<Map<String, Object>> itemViews = new ArrayList<>(staticItems.size());
        for (PlanItemStaticView item : staticItems) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", item.id());
            row.put("planId", item.planId());
            row.put("dayIndex", item.dayIndex());
            row.put("actionName", item.actionName());
            row.put("sets", item.sets());
            row.put("reps", item.reps());
            row.put("durationMin", item.durationMin());
            row.put("restSec", item.restSec());
            row.put("sort", item.sort());

            PlanVideoStaticView video = item.video();
            if (video != null) {
                Map<String, Object> videoInfo = new HashMap<>();
                videoInfo.put("videoId", video.videoId());
                videoInfo.put("title", video.title());
                videoInfo.put("durationSec", video.durationSec());
                videoInfo.put("tags", video.tags());
                videoInfo.put("playUrl", buildPlayableUrl(video));
                row.put("video", videoInfo);
            } else {
                row.put("video", null);
            }
            itemViews.add(row);
        }
        return itemViews;
    }

    private Map<Long, TrainingPlanSubscribe> loadUserSubscribeMap(Long userId) {
        if (userId == null) {
            return Map.of();
        }
        return trainingPlanSubscribeMapper.selectList(
                        new LambdaQueryWrapper<TrainingPlanSubscribe>()
                                .eq(TrainingPlanSubscribe::getUserId, userId)
                                .eq(TrainingPlanSubscribe::getStatus, 1)
                ).stream()
                .collect(Collectors.toMap(TrainingPlanSubscribe::getPlanId, item -> item, (left, right) -> right));
    }

    private TrainingPlanSubscribe loadPlanSubscribe(Long userId, Long planId) {
        if (userId == null || planId == null) {
            return null;
        }
        return trainingPlanSubscribeMapper.selectOne(
                new LambdaQueryWrapper<TrainingPlanSubscribe>()
                        .eq(TrainingPlanSubscribe::getPlanId, planId)
                        .eq(TrainingPlanSubscribe::getUserId, userId)
                        .last("LIMIT 1")
        );
    }

    private String buildPlayableUrl(PlanVideoStaticView video) {
        if (video == null) {
            return null;
        }

        if (StringUtils.hasText(video.minioPath())) {
            if (video.minioPath().startsWith("http://") || video.minioPath().startsWith("https://")) {
                return video.minioPath();
            }
            String path = video.minioPath().startsWith("/")
                    ? video.minioPath().substring(1)
                    : video.minioPath();

            if (minioClient != null) {
                try {
                    return minioClient.getPresignedObjectUrl(
                            GetPresignedObjectUrlArgs.builder()
                                    .method(Method.GET)
                                    .bucket(minioBucketName)
                                    .object(path)
                                    .expiry(2, TimeUnit.HOURS)
                                    .build()
                    );
                } catch (Exception ignored) {
                    // fallback to normal path url
                }
            }

            String endpoint = minioPublicEndpoint.endsWith("/")
                    ? minioPublicEndpoint.substring(0, minioPublicEndpoint.length() - 1)
                    : minioPublicEndpoint;
            return endpoint + "/" + minioBucketName + "/" + path;
        }

        return video.sourceUrl();
    }

    private record PlanListStaticView(
            Long id,
            String title,
            String goal,
            String level,
            Integer durationWeeks,
            String coverUrl,
            Integer status
    ) {
    }

    private record PlanDetailStaticCache(
            TrainingPlan plan,
            List<PlanItemStaticView> items
    ) {
    }

    private record PlanItemStaticView(
            Long id,
            Long planId,
            Integer dayIndex,
            String actionName,
            Integer sets,
            Integer reps,
            Integer durationMin,
            Integer restSec,
            Integer sort,
            PlanVideoStaticView video
    ) {
    }

    private record PlanVideoStaticView(
            Long videoId,
            String title,
            Integer durationSec,
            String tags,
            String minioPath,
            String sourceUrl
    ) {
    }
}
