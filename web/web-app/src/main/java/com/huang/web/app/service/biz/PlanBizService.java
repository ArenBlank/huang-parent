package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    @Value("${minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;

    @Value("${minio.bucket-name:fitness-platform}")
    private String minioBucketName;

    public PlanBizService(TrainingPlanMapper trainingPlanMapper,
                          TrainingPlanItemMapper trainingPlanItemMapper,
                          TrainingPlanSubscribeMapper trainingPlanSubscribeMapper,
                          VideoAssetMapper videoAssetMapper,
                          ObjectProvider<MinioClient> minioClientProvider) {
        this.trainingPlanMapper = trainingPlanMapper;
        this.trainingPlanItemMapper = trainingPlanItemMapper;
        this.trainingPlanSubscribeMapper = trainingPlanSubscribeMapper;
        this.videoAssetMapper = videoAssetMapper;
        this.minioClient = minioClientProvider.getIfAvailable();
    }

    public List<TrainingPlan> listActivePlans() {
        return trainingPlanMapper.selectList(
                new LambdaQueryWrapper<TrainingPlan>()
                        .eq(TrainingPlan::getStatus, 1)
                        .orderByDesc(TrainingPlan::getId)
        );
    }

    public Map<String, Object> getPlanDetail(Long planId) {
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
            // 工程亮点：批量查询视频素材，避免 N+1 查询。
            videoMap = videoAssetMapper.selectBatchIds(videoIds).stream()
                    .collect(Collectors.toMap(VideoAsset::getId, v -> v));
        }

        List<Map<String, Object>> itemViews = new ArrayList<>(items.size());
        for (TrainingPlanItem item : items) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", item.getId());
            row.put("dayIndex", item.getDayIndex());
            row.put("actionName", item.getActionName());
            row.put("sets", item.getSets());
            row.put("reps", item.getReps());
            row.put("durationMin", item.getDurationMin());
            row.put("restSec", item.getRestSec());
            row.put("sort", item.getSort());

            VideoAsset video = videoMap.get(item.getVideoId());
            if (video != null) {
                Map<String, Object> videoInfo = new HashMap<>();
                videoInfo.put("videoId", video.getId());
                videoInfo.put("title", video.getTitle());
                videoInfo.put("durationSec", video.getDurationSec());
                videoInfo.put("tags", video.getTags());
                videoInfo.put("playUrl", buildPlayableUrl(video));
                row.put("video", videoInfo);
            } else {
                row.put("video", null);
            }

            itemViews.add(row);
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("plan", plan);
        detail.put("items", itemViews);
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean subscribe(Long userId, PlanSubscribeDTO dto) {
        TrainingPlan plan = trainingPlanMapper.selectById(dto.getPlanId());
        if (plan == null || plan.getStatus() == null || plan.getStatus() != 1) {
            return false;
        }

        TrainingPlanSubscribe existed = trainingPlanSubscribeMapper.selectOne(
                new LambdaQueryWrapper<TrainingPlanSubscribe>()
                        .eq(TrainingPlanSubscribe::getPlanId, dto.getPlanId())
                        .eq(TrainingPlanSubscribe::getUserId, userId)
                        .last("LIMIT 1")
        );
        if (existed != null) {
            // 工程亮点：幂等更新，重复订阅不产生脏数据。
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

    private String buildPlayableUrl(VideoAsset videoAsset) {
        if (videoAsset == null) {
            return null;
        }

        if (StringUtils.hasText(videoAsset.getMinioPath())) {
            if (videoAsset.getMinioPath().startsWith("http://") || videoAsset.getMinioPath().startsWith("https://")) {
                return videoAsset.getMinioPath();
            }
            String path = videoAsset.getMinioPath().startsWith("/")
                    ? videoAsset.getMinioPath().substring(1)
                    : videoAsset.getMinioPath();

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

            String endpoint = minioEndpoint.endsWith("/")
                    ? minioEndpoint.substring(0, minioEndpoint.length() - 1)
                    : minioEndpoint;
            return endpoint + "/" + minioBucketName + "/" + path;
        }

        return videoAsset.getSourceUrl();
    }
}
