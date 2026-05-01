package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.huang.common.constant.RedisConstant;
import com.huang.common.exception.HuangException;
import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.common.redis.MultiLevelCacheSupport;
import com.huang.common.redis.RedisCacheSupport;
import com.huang.common.result.ResultCodeEnum;
import com.huang.model.entity.TrainingPlan;
import com.huang.model.entity.TrainingPlanItem;
import com.huang.model.entity.TrainingPlanSubscribe;
import com.huang.model.entity.VideoAsset;
import com.huang.web.app.ai.AiGeneratedPlan;
import com.huang.web.app.ai.AiGeneratedPlanItem;
import com.huang.web.app.ai.PersonalTrainerAi;
import com.huang.web.app.dto.plan.PlanSubscribeDTO;
import com.huang.web.app.mapper.TrainingPlanItemMapper;
import com.huang.web.app.mapper.TrainingPlanMapper;
import com.huang.web.app.mapper.TrainingPlanSubscribeMapper;
import com.huang.web.app.mapper.VideoAssetMapper;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlanBizService {

    private static final int TRAINING_PLAN_TITLE_MAX_LEN = 100;
    private static final int TRAINING_PLAN_GOAL_MAX_LEN = 50;
    private static final int TRAINING_PLAN_ITEM_ACTION_MAX_LEN = 100;
    private static final int STATIC_PLAN_CACHE_WAIT_RETRY_TIMES = 4;
    private static final long STATIC_PLAN_CACHE_WAIT_BASE_MS = 40L;
    private static final long STATIC_PLAN_CACHE_WAIT_JITTER_MS = 80L;

    private static final String AI_BUSY_MESSAGE = "AI 教练正在思考中，请稍后再试";
    private static final String PLAN_TYPE_PUBLIC = "PUBLIC";
    private static final String PLAN_TYPE_AI_PRIVATE = "AI_PRIVATE";

    private static final List<String> ACTION_NAME_NOISE_TOKENS = List.of(
            "标准", "传统", "基础", "入门", "进阶", "训练", "动作", "教学", "教程", "示范", "练习", "版本", "式"
    );

    private static final List<String> CORE_ACTION_KEYWORDS = List.of(
            "高位下拉", "引体向上", "平板支撑", "俄罗斯转体", "哑铃推举", "杠铃深蹲", "标准俯卧撑",
            "箭步蹲", "波比跳", "卷腹", "臀桥", "划船", "推举", "卧推", "硬拉", "深蹲", "俯卧撑"
    );

    private final TrainingPlanMapper trainingPlanMapper;
    private final TrainingPlanItemMapper trainingPlanItemMapper;
    private final TrainingPlanSubscribeMapper trainingPlanSubscribeMapper;
    private final VideoAssetMapper videoAssetMapper;
    private final MinioClient minioClient;
    private final MultiLevelCacheSupport multiLevelCacheSupport;
    private final PersonalTrainerAi personalTrainerAi;

    @Value("${minio.public-endpoint:}")
    private String minioPublicEndpoint;

    @Value("${minio.bucket-name:fitness-platform}")
    private String minioBucketName;

    public PlanBizService(TrainingPlanMapper trainingPlanMapper,
                          TrainingPlanItemMapper trainingPlanItemMapper,
                          TrainingPlanSubscribeMapper trainingPlanSubscribeMapper,
                          VideoAssetMapper videoAssetMapper,
                          ObjectProvider<MinioClient> minioClientProvider,
                          MultiLevelCacheSupport multiLevelCacheSupport,
                          PersonalTrainerAi personalTrainerAi) {
        this.trainingPlanMapper = trainingPlanMapper;
        this.trainingPlanItemMapper = trainingPlanItemMapper;
        this.trainingPlanSubscribeMapper = trainingPlanSubscribeMapper;
        this.videoAssetMapper = videoAssetMapper;
        this.minioClient = minioClientProvider.getIfAvailable();
        this.multiLevelCacheSupport = multiLevelCacheSupport;
        this.personalTrainerAi = personalTrainerAi;
    }

    public Map<String, Object> getPlanOverview(Long userId) {
        List<TrainingPlan> visiblePlans = loadVisibleActivePlans(userId);
        Map<Long, TrainingPlanSubscribe> activeSubscribeMap = loadUserSubscribeMap(userId);

        List<Map<String, Object>> myPlans = new ArrayList<>();
        List<Map<String, Object>> libraryPlans = new ArrayList<>();
        for (TrainingPlan plan : visiblePlans) {
            TrainingPlanSubscribe subscribe = activeSubscribeMap.get(plan.getId());
            Map<String, Object> row = buildPlanOverviewRow(plan, subscribe, userId);
            if (subscribe != null) {
                myPlans.add(row);
            } else {
                libraryPlans.add(row);
            }
        }

        myPlans.sort(Comparator
                .comparing((Map<String, Object> item) -> (LocalDate) item.get("startDate"), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(item -> (Long) item.get("id"), Comparator.reverseOrder()));
        libraryPlans.sort(Comparator
                .comparing((Map<String, Object> item) -> !(Boolean) item.get("ownedByCurrentUser"))
                .thenComparing(item -> (Long) item.get("id"), Comparator.reverseOrder()));

        Long currentPlanId = null;
        if (!myPlans.isEmpty()) {
            currentPlanId = (Long) myPlans.get(0).get("id");
        } else if (!libraryPlans.isEmpty()) {
            currentPlanId = (Long) libraryPlans.get(0).get("id");
        }

        Map<String, Object> overview = new HashMap<>();
        overview.put("myPlans", myPlans);
        overview.put("libraryPlans", libraryPlans);
        overview.put("currentPlanId", currentPlanId);
        return overview;
    }

    public List<Map<String, Object>> listActivePlans(Long userId) {
        List<PlanListStaticView> staticRows = loadActivePlanStatics();
        if (staticRows.isEmpty()) {
            return List.of();
        }

        Map<Long, TrainingPlanSubscribe> subscribeMap = loadUserSubscribeMap(userId);
        return staticRows.stream()
                .map(plan -> {
                    TrainingPlanSubscribe subscribe = subscribeMap.get(plan.id());
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", plan.id());
                    row.put("title", plan.title());
                    row.put("goal", plan.goal());
                    row.put("level", plan.level());
                    row.put("durationWeeks", plan.durationWeeks());
                    row.put("coverUrl", plan.coverUrl());
                    row.put("status", plan.status());
                    row.put("subscribed", subscribe != null && subscribe.getStatus() != null && subscribe.getStatus() == 1);
                    row.put("startDate", subscribe == null ? null : subscribe.getStartDate());
                    row.put("ownedByCurrentUser", false);
                    row.put("planType", PLAN_TYPE_PUBLIC);
                    return row;
                })
                .toList();
    }

    public Map<String, Object> getPlanDetail(Long planId, Long userId) {
        if (planId == null || planId <= 0) {
            return null;
        }

        PlanDetailStaticCache staticDetail = getStaticPlanDetail(planId);
        if (staticDetail == null || !canAccessPlan(staticDetail.plan(), userId)) {
            return null;
        }

        TrainingPlanSubscribe subscribe = userId == null ? null : loadActivePlanSubscribe(userId, planId);

        Map<String, Object> detail = new HashMap<>();
        detail.put("plan", staticDetail.plan());
        detail.put("items", buildDetailItems(staticDetail.items()));
        detail.put("subscription", buildSubscriptionView(subscribe));
        detail.put("ownedByCurrentUser", isOwnedByCurrentUser(staticDetail.plan(), userId));
        detail.put("planType", resolvePlanType(staticDetail.plan(), userId));
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean subscribe(Long userId, PlanSubscribeDTO dto) {
        TrainingPlan plan = findVisibleActivePlan(dto.getPlanId(), userId);
        if (plan == null) {
            return false;
        }
        return upsertSubscription(userId, dto.getPlanId(), dto.getStartDate(), 1);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean unsubscribe(Long userId, Long planId) {
        TrainingPlanSubscribe existed = loadExistingPlanSubscribe(userId, planId);
        if (existed == null || existed.getStatus() == null || existed.getStatus() == 0) {
            return true;
        }
        existed.setStatus(0);
        return trainingPlanSubscribeMapper.updateById(existed) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateAndSavePlanByAi(String userPrompt) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new HuangException(ResultCodeEnum.APP_LOGIN_AUTH);
        }

        AiGeneratedPlan aiPlan = loadAiPlanSafely(userPrompt, loginUser);

        TrainingPlan plan = new TrainingPlan();
        plan.setTitle(normalizeText(aiPlan.planName(), TRAINING_PLAN_TITLE_MAX_LEN));
        plan.setGoal(normalizeText(aiPlan.description(), TRAINING_PLAN_GOAL_MAX_LEN));
        plan.setLevel(normalizeDifficulty(aiPlan.difficulty()));
        plan.setDurationWeeks(resolveDurationWeeks(aiPlan));
        plan.setCoverUrl(null);
        plan.setStatus(1);
        plan.setOwnerUserId(loginUser.getUserId());
        trainingPlanMapper.insert(plan);

        List<AiGeneratedPlanItem> items = aiPlan.items().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(AiGeneratedPlanItem::dayIndex))
                .toList();

        Map<String, Long> matchedVideoIds = resolveVideoIdsForAiItems(items);
        Map<Integer, Integer> sortCounter = new HashMap<>();
        for (AiGeneratedPlanItem item : items) {
            int dayIndex = safePositive(item.dayIndex());
            int sort = sortCounter.merge(dayIndex, 1, Integer::sum);
            String actionName = normalizeText(item.actionName(), TRAINING_PLAN_ITEM_ACTION_MAX_LEN);

            TrainingPlanItem entity = new TrainingPlanItem();
            entity.setPlanId(plan.getId());
            entity.setDayIndex(dayIndex);
            entity.setActionName(actionName);
            entity.setSets(safeNonNegative(item.sets()));
            entity.setReps(safeNonNegative(item.reps()));
            entity.setDurationMin(safeNonNegative(item.durationMin()));
            entity.setRestSec(safeNonNegative(item.restSec()));
            entity.setVideoId(matchedVideoIds.get(actionName));
            entity.setSort(sort);
            trainingPlanItemMapper.insert(entity);
        }

        upsertSubscription(loginUser.getUserId(), plan.getId(), LocalDate.now(), 1);

        clearPlanListCache();
        clearPlanDetailCache(plan.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("planId", plan.getId());
        result.put("title", plan.getTitle());
        result.put("itemCount", items.size());
        return result;
    }

    private List<TrainingPlan> loadVisibleActivePlans(Long userId) {
        LambdaQueryWrapper<TrainingPlan> wrapper = new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getStatus, 1)
                .orderByDesc(TrainingPlan::getId);
        if (userId == null) {
            wrapper.isNull(TrainingPlan::getOwnerUserId);
        } else {
            wrapper.and(query -> query.isNull(TrainingPlan::getOwnerUserId)
                    .or()
                    .eq(TrainingPlan::getOwnerUserId, userId));
        }
        return trainingPlanMapper.selectList(wrapper);
    }

    private Map<String, Object> buildPlanOverviewRow(TrainingPlan plan, TrainingPlanSubscribe subscribe, Long userId) {
        boolean ownedByCurrentUser = isOwnedByCurrentUser(plan, userId);
        Map<String, Object> row = new HashMap<>();
        row.put("id", plan.getId());
        row.put("title", plan.getTitle());
        row.put("goal", plan.getGoal());
        row.put("level", plan.getLevel());
        row.put("durationWeeks", plan.getDurationWeeks());
        row.put("coverUrl", plan.getCoverUrl());
        row.put("status", plan.getStatus());
        row.put("subscribed", subscribe != null && subscribe.getStatus() != null && subscribe.getStatus() == 1);
        row.put("startDate", subscribe == null ? null : subscribe.getStartDate());
        row.put("ownedByCurrentUser", ownedByCurrentUser);
        row.put("planType", ownedByCurrentUser ? PLAN_TYPE_AI_PRIVATE : PLAN_TYPE_PUBLIC);
        return row;
    }

    private List<PlanListStaticView> loadActivePlanStatics() {
        RedisCacheSupport.CacheValue<List<PlanListStaticView>> cached = multiLevelCacheSupport.getJson(
                RedisConstant.APP_PLAN_LIST_ACTIVE_KEY,
                new TypeReference<List<PlanListStaticView>>() {}
        );
        if (cached.found()) {
            return cached.nullValue() ? List.of() : cached.value();
        }

        List<TrainingPlan> plans = trainingPlanMapper.selectList(
                new LambdaQueryWrapper<TrainingPlan>()
                        .eq(TrainingPlan::getStatus, 1)
                        .isNull(TrainingPlan::getOwnerUserId)
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
        RedisCacheSupport.CacheValue<PlanDetailStaticCache> cached = multiLevelCacheSupport.getJson(
                cacheKey,
                new TypeReference<PlanDetailStaticCache>() {}
        );
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
            RedisCacheSupport.CacheValue<PlanDetailStaticCache> secondRead = multiLevelCacheSupport.getJson(
                    cacheKey,
                    new TypeReference<PlanDetailStaticCache>() {}
            );
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
        for (int i = 0; i < STATIC_PLAN_CACHE_WAIT_RETRY_TIMES; i++) {
            try {
                TimeUnit.MILLISECONDS.sleep(nextStaticPlanCacheWaitMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            RedisCacheSupport.CacheValue<PlanDetailStaticCache> cached = multiLevelCacheSupport.getJson(
                    cacheKey,
                    new TypeReference<PlanDetailStaticCache>() {}
            );
            if (!cached.found()) {
                continue;
            }
            return cached.nullValue() ? null : cached.value();
        }
        return null;
    }

    private long nextStaticPlanCacheWaitMs() {
        return STATIC_PLAN_CACHE_WAIT_BASE_MS
                + ThreadLocalRandom.current().nextLong(STATIC_PLAN_CACHE_WAIT_JITTER_MS + 1);
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
                    .collect(Collectors.toMap(VideoAsset::getId, item -> item));
        }

        List<PlanItemStaticView> itemViews = new ArrayList<>(items.size());
        for (TrainingPlanItem item : items) {
            VideoAsset video = item.getVideoId() == null ? null : videoMap.get(item.getVideoId());
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

    private Map<String, Object> buildSubscriptionView(TrainingPlanSubscribe subscribe) {
        if (subscribe == null) {
            return null;
        }
        Map<String, Object> subscription = new HashMap<>();
        subscription.put("planId", subscribe.getPlanId());
        subscription.put("startDate", subscribe.getStartDate());
        subscription.put("status", subscribe.getStatus());
        return subscription;
    }

    private boolean canAccessPlan(TrainingPlan plan, Long userId) {
        if (plan == null || plan.getStatus() == null || plan.getStatus() != 1) {
            return false;
        }
        return plan.getOwnerUserId() == null || Objects.equals(plan.getOwnerUserId(), userId);
    }

    private boolean isOwnedByCurrentUser(TrainingPlan plan, Long userId) {
        return userId != null && plan != null && Objects.equals(plan.getOwnerUserId(), userId);
    }

    private String resolvePlanType(TrainingPlan plan, Long userId) {
        return isOwnedByCurrentUser(plan, userId) ? PLAN_TYPE_AI_PRIVATE : PLAN_TYPE_PUBLIC;
    }

    private TrainingPlan findVisibleActivePlan(Long planId, Long userId) {
        if (planId == null || planId <= 0) {
            return null;
        }
        TrainingPlan plan = trainingPlanMapper.selectById(planId);
        return canAccessPlan(plan, userId) ? plan : null;
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

    private TrainingPlanSubscribe loadExistingPlanSubscribe(Long userId, Long planId) {
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

    private TrainingPlanSubscribe loadActivePlanSubscribe(Long userId, Long planId) {
        if (userId == null || planId == null) {
            return null;
        }
        return trainingPlanSubscribeMapper.selectOne(
                new LambdaQueryWrapper<TrainingPlanSubscribe>()
                        .eq(TrainingPlanSubscribe::getPlanId, planId)
                        .eq(TrainingPlanSubscribe::getUserId, userId)
                        .eq(TrainingPlanSubscribe::getStatus, 1)
                        .last("LIMIT 1")
        );
    }

    private boolean upsertSubscription(Long userId, Long planId, LocalDate startDate, int status) {
        TrainingPlanSubscribe existed = loadExistingPlanSubscribe(userId, planId);
        if (existed != null) {
            existed.setStartDate(startDate);
            existed.setStatus(status);
            return trainingPlanSubscribeMapper.updateById(existed) > 0;
        }

        TrainingPlanSubscribe subscribe = new TrainingPlanSubscribe();
        subscribe.setPlanId(planId);
        subscribe.setUserId(userId);
        subscribe.setStartDate(startDate);
        subscribe.setStatus(status);
        return trainingPlanSubscribeMapper.insert(subscribe) > 0;
    }

    private void clearPlanListCache() {
        multiLevelCacheSupport.sharedEvict(RedisConstant.APP_PLAN_LIST_ACTIVE_KEY);
    }

    private void clearPlanDetailCache(Long planId) {
        if (planId != null) {
            multiLevelCacheSupport.sharedEvict(RedisConstant.appPlanDetailStaticKey(planId));
        }
    }

    private AiGeneratedPlan loadAiPlanSafely(String userPrompt, LoginUser loginUser) {
        try {
            AiGeneratedPlan generatedPlan = personalTrainerAi.generatePlan(userPrompt == null ? "" : userPrompt.trim());
            validateAiPlan(generatedPlan, loginUser);
            return generatedPlan;
        } catch (HuangException e) {
            throw e;
        } catch (Exception e) {
            log.warn("ai plan generation failed, userId={}, prompt={}", loginUser.getUserId(), userPrompt, e);
            throw new HuangException(ResultCodeEnum.SERVICE_ERROR.getCode(), AI_BUSY_MESSAGE);
        }
    }

    private void validateAiPlan(AiGeneratedPlan aiPlan, LoginUser loginUser) {
        if (aiPlan == null || !StringUtils.hasText(aiPlan.planName())) {
            log.warn("ai plan invalid, missing title, userId={}", loginUser.getUserId());
            throw new HuangException(ResultCodeEnum.SERVICE_ERROR.getCode(), AI_BUSY_MESSAGE);
        }
        if (aiPlan.items() == null || aiPlan.items().isEmpty()) {
            log.warn("ai plan invalid, empty items, userId={}", loginUser.getUserId());
            throw new HuangException(ResultCodeEnum.SERVICE_ERROR.getCode(), AI_BUSY_MESSAGE);
        }

        for (AiGeneratedPlanItem item : aiPlan.items()) {
            if (item == null || item.dayIndex() == null || item.dayIndex() < 1 || !StringUtils.hasText(item.actionName())) {
                log.warn("ai plan invalid, bad item payload, userId={}, item={}", loginUser.getUserId(), item);
                throw new HuangException(ResultCodeEnum.SERVICE_ERROR.getCode(), AI_BUSY_MESSAGE);
            }
        }
    }

    private Map<String, Long> resolveVideoIdsForAiItems(List<AiGeneratedPlanItem> items) {
        LinkedHashSet<String> actionNames = items.stream()
                .map(AiGeneratedPlanItem::actionName)
                .map(action -> normalizeText(action, TRAINING_PLAN_ITEM_ACTION_MAX_LEN))
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, Long> result = new HashMap<>();
        for (String actionName : actionNames) {
            Long matchedVideoId = findMatchedVideoId(actionName);
            result.put(actionName, matchedVideoId);
            if (matchedVideoId != null) {
                log.info("matched ai action to video, actionName={}, videoId={}", actionName, matchedVideoId);
            } else {
                log.info("no video matched for ai action, actionName={}", actionName);
            }
        }
        return result;
    }

    private Long findMatchedVideoId(String actionName) {
        for (String keyword : buildVideoMatchKeywords(actionName)) {
            List<VideoAsset> matchedAssets = videoAssetMapper.selectList(
                    new LambdaQueryWrapper<VideoAsset>()
                            .eq(VideoAsset::getStatus, 1)
                            .like(VideoAsset::getTitle, keyword)
                            .orderByAsc(VideoAsset::getId)
                            .last("LIMIT 1")
            );
            if (matchedAssets != null && !matchedAssets.isEmpty()) {
                return matchedAssets.get(0).getId();
            }
        }
        return null;
    }

    private List<String> buildVideoMatchKeywords(String actionName) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        String normalized = normalizeText(actionName, TRAINING_PLAN_ITEM_ACTION_MAX_LEN);
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }

        keywords.add(normalized);

        String plain = normalized
                .replace("（", " ")
                .replace("）", " ")
                .replace("(", " ")
                .replace(")", " ")
                .replace("/", " ")
                .replace("、", " ")
                .replace("，", " ")
                .replace(",", " ")
                .replace("·", " ")
                .replace("-", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (StringUtils.hasText(plain)) {
            keywords.add(plain);
        }

        String stripped = plain.replace(" ", "");
        for (String noise : ACTION_NAME_NOISE_TOKENS) {
            stripped = stripped.replace(noise, "");
        }
        if (StringUtils.hasText(stripped) && stripped.length() >= 2) {
            keywords.add(stripped);
        }

        for (String coreKeyword : CORE_ACTION_KEYWORDS) {
            if (plain.contains(coreKeyword) || stripped.contains(coreKeyword)) {
                keywords.add(coreKeyword);
            }
        }

        return keywords.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String normalizeText(String text) {
        return normalizeText(text, Integer.MAX_VALUE);
    }

    private String normalizeText(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = text.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String normalizeDifficulty(String difficulty) {
        if (!StringUtils.hasText(difficulty)) {
            return "beginner";
        }
        String normalized = difficulty.trim().toLowerCase();
        return switch (normalized) {
            case "beginner", "intermediate", "advanced" -> normalized;
            default -> "beginner";
        };
    }

    private Integer resolveDurationWeeks(AiGeneratedPlan aiPlan) {
        if (aiPlan.durationWeeks() != null && aiPlan.durationWeeks() > 0) {
            return aiPlan.durationWeeks();
        }

        int maxDay = aiPlan.items().stream()
                .filter(Objects::nonNull)
                .map(AiGeneratedPlanItem::dayIndex)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(1);
        return Math.max(1, (maxDay + 6) / 7);
    }

    private int safePositive(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    private int safeNonNegative(Integer value) {
        return value == null || value < 0 ? 0 : value;
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
                    // fallback to public path
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
