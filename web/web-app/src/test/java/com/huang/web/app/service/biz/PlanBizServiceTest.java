package com.huang.web.app.service.biz;

import com.huang.common.constant.RedisConstant;
import com.huang.common.redis.RedisCacheSupport;
import com.huang.model.entity.TrainingPlan;
import com.huang.model.entity.TrainingPlanItem;
import com.huang.model.entity.TrainingPlanSubscribe;
import com.huang.model.entity.VideoAsset;
import com.huang.web.app.mapper.TrainingPlanItemMapper;
import com.huang.web.app.mapper.TrainingPlanMapper;
import com.huang.web.app.mapper.TrainingPlanSubscribeMapper;
import com.huang.web.app.mapper.VideoAssetMapper;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanBizServiceTest {

    @Mock
    private TrainingPlanMapper trainingPlanMapper;

    @Mock
    private TrainingPlanItemMapper trainingPlanItemMapper;

    @Mock
    private TrainingPlanSubscribeMapper trainingPlanSubscribeMapper;

    @Mock
    private VideoAssetMapper videoAssetMapper;

    @Mock
    private ObjectProvider<MinioClient> minioClientProvider;

    @Mock
    private MinioClient minioClient;

    @Mock
    private RedisCacheSupport redisCacheSupport;

    private PlanBizService planBizService;

    @BeforeEach
    void setUp() {
        when(minioClientProvider.getIfAvailable()).thenReturn(minioClient);
        planBizService = new PlanBizService(
                trainingPlanMapper,
                trainingPlanItemMapper,
                trainingPlanSubscribeMapper,
                videoAssetMapper,
                minioClientProvider,
                redisCacheSupport
        );
        ReflectionTestUtils.setField(planBizService, "minioPublicEndpoint", "http://files.localhost");
        ReflectionTestUtils.setField(planBizService, "minioBucketName", "fitness-platform");
    }

    @Test
    void listActivePlans_shouldCacheStaticRowsAndMergeUserSubscription() {
        TrainingPlan plan = buildPlan(1L, "Plan A");
        TrainingPlanSubscribe subscribe = new TrainingPlanSubscribe();
        subscribe.setPlanId(1L);
        subscribe.setUserId(99L);
        subscribe.setStatus(1);
        subscribe.setStartDate(LocalDate.of(2026, 4, 7));

        when(redisCacheSupport.getJson(eq(RedisConstant.APP_PLAN_LIST_ACTIVE_KEY), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(RedisCacheSupport.CacheValue.miss());
        when(trainingPlanMapper.selectList(any())).thenReturn(List.of(plan));
        when(trainingPlanSubscribeMapper.selectList(any())).thenReturn(List.of(subscribe));
        when(redisCacheSupport.ttlWithJitter(RedisConstant.APP_PLAN_LIST_TTL_SEC, RedisConstant.JITTER_SHORT_SEC))
                .thenReturn(RedisConstant.APP_PLAN_LIST_TTL_SEC);

        List<Map<String, Object>> result = planBizService.listActivePlans(99L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0))
                .containsEntry("id", 1L)
                .containsEntry("title", "Plan A")
                .containsEntry("subscribed", true)
                .containsEntry("startDate", LocalDate.of(2026, 4, 7));
        verify(redisCacheSupport).setJson(eq(RedisConstant.APP_PLAN_LIST_ACTIVE_KEY), any(), eq(RedisConstant.APP_PLAN_LIST_TTL_SEC));
    }

    @Test
    void getPlanDetail_shouldBuildStaticCacheFromDbAndAssembleDynamicFields() throws Exception {
        TrainingPlan plan = buildPlan(2L, "Plan B");
        TrainingPlanItem item = new TrainingPlanItem();
        item.setId(21L);
        item.setPlanId(2L);
        item.setDayIndex(1);
        item.setActionName("Squat");
        item.setSets(4);
        item.setReps(12);
        item.setDurationMin(20);
        item.setRestSec(60);
        item.setSort(1);
        item.setVideoId(7L);

        VideoAsset videoAsset = new VideoAsset();
        videoAsset.setId(7L);
        videoAsset.setTitle("Squat Demo");
        videoAsset.setDurationSec(45);
        videoAsset.setTags("legs");
        videoAsset.setMinioPath("videos/demo.mp4");

        TrainingPlanSubscribe subscribe = new TrainingPlanSubscribe();
        subscribe.setPlanId(2L);
        subscribe.setUserId(101L);
        subscribe.setStatus(1);
        subscribe.setStartDate(LocalDate.of(2026, 4, 8));

        String cacheKey = RedisConstant.appPlanDetailStaticKey(2L);
        when(redisCacheSupport.getJson(eq(cacheKey), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(RedisCacheSupport.CacheValue.miss(), RedisCacheSupport.CacheValue.miss());
        when(redisCacheSupport.newLockToken()).thenReturn("lock-token");
        when(redisCacheSupport.tryLock(RedisConstant.appPlanDetailLockKey(2L), "lock-token", RedisConstant.CACHE_LOCK_TTL_SEC))
                .thenReturn(true);
        when(trainingPlanMapper.selectById(2L)).thenReturn(plan);
        when(trainingPlanItemMapper.selectList(any())).thenReturn(List.of(item));
        when(videoAssetMapper.selectBatchIds(any())).thenReturn(List.of(videoAsset));
        when(trainingPlanSubscribeMapper.selectOne(any())).thenReturn(subscribe);
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn("https://signed/video");
        when(redisCacheSupport.ttlWithJitter(RedisConstant.APP_PLAN_DETAIL_TTL_SEC, RedisConstant.JITTER_SHORT_SEC))
                .thenReturn(RedisConstant.APP_PLAN_DETAIL_TTL_SEC);

        Map<String, Object> detail = planBizService.getPlanDetail(2L, 101L);

        assertThat(detail).isNotNull();
        assertThat(((TrainingPlan) detail.get("plan")).getTitle()).isEqualTo("Plan B");
        assertThat((Map<String, Object>) detail.get("subscription"))
                .containsEntry("planId", 2L)
                .containsEntry("startDate", LocalDate.of(2026, 4, 8))
                .containsEntry("status", 1);
        List<Map<String, Object>> items = (List<Map<String, Object>>) detail.get("items");
        assertThat(items).hasSize(1);
        assertThat((Map<String, Object>) items.get(0).get("video"))
                .containsEntry("videoId", 7L)
                .containsEntry("title", "Squat Demo")
                .containsEntry("playUrl", "https://signed/video");
        verify(redisCacheSupport).setJson(eq(cacheKey), any(), eq(RedisConstant.APP_PLAN_DETAIL_TTL_SEC));
        verify(redisCacheSupport).unlock(RedisConstant.appPlanDetailLockKey(2L), "lock-token");
    }

    @Test
    void getPlanDetail_shouldReuseCachedStaticDataButStillLoadSubscriptionAndGeneratePlayUrl() throws Exception {
        Object cachedStatic = createStaticDetailCache();
        TrainingPlanSubscribe subscribe = new TrainingPlanSubscribe();
        subscribe.setPlanId(3L);
        subscribe.setUserId(202L);
        subscribe.setStatus(1);
        subscribe.setStartDate(LocalDate.of(2026, 4, 9));

        String cacheKey = RedisConstant.appPlanDetailStaticKey(3L);
        when(redisCacheSupport.getJson(eq(cacheKey), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(RedisCacheSupport.CacheValue.hit(cachedStatic));
        when(trainingPlanSubscribeMapper.selectOne(any())).thenReturn(subscribe);
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn("https://signed/cached");

        Map<String, Object> detail = planBizService.getPlanDetail(3L, 202L);

        assertThat(detail).isNotNull();
        assertThat(((TrainingPlan) detail.get("plan")).getTitle()).isEqualTo("Cached Plan");
        assertThat((Map<String, Object>) detail.get("subscription"))
                .containsEntry("planId", 3L)
                .containsEntry("status", 1);
        List<Map<String, Object>> items = (List<Map<String, Object>>) detail.get("items");
        assertThat((Map<String, Object>) items.get(0).get("video"))
                .containsEntry("playUrl", "https://signed/cached");
        verify(trainingPlanMapper, never()).selectById(anyLong());
        verify(trainingPlanItemMapper, never()).selectList(any());
        verify(videoAssetMapper, never()).selectBatchIds(any());
    }

    private TrainingPlan buildPlan(Long id, String title) {
        TrainingPlan plan = new TrainingPlan();
        plan.setId(id);
        plan.setTitle(title);
        plan.setGoal("Gain strength");
        plan.setLevel("beginner");
        plan.setDurationWeeks(8);
        plan.setCoverUrl("cover.png");
        plan.setStatus(1);
        return plan;
    }

    private Object createStaticDetailCache() throws Exception {
        TrainingPlan plan = buildPlan(3L, "Cached Plan");
        Object videoView = instantiateInnerRecord(
                "PlanVideoStaticView",
                88L,
                "Cached Video",
                30,
                "core",
                "videos/cached.mp4",
                "https://origin/video"
        );
        Object itemView = instantiateInnerRecord(
                "PlanItemStaticView",
                301L,
                3L,
                1,
                "Plank",
                3,
                1,
                10,
                30,
                1,
                videoView
        );
        return instantiateInnerRecord(
                "PlanDetailStaticCache",
                plan,
                List.of(itemView)
        );
    }

    private Object instantiateInnerRecord(String simpleName, Object... args) throws Exception {
        Class<?> clazz = Class.forName(PlanBizService.class.getName() + "$" + simpleName);
        Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        return constructor.newInstance(args);
    }
}
