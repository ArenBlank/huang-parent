package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.minio.MinioProperties;
import com.huang.model.entity.TrainingPlanItem;
import com.huang.model.entity.VideoAsset;
import com.huang.web.admin.dto.video.VideoAssetUpsertDTO;
import com.huang.web.admin.mapper.TrainingPlanItemMapper;
import com.huang.web.admin.mapper.VideoAssetMapper;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class VideoContentBizService {

    private final VideoAssetMapper videoAssetMapper;
    private final TrainingPlanItemMapper trainingPlanItemMapper;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Value("${minio.public-endpoint:http://files.localhost}")
    private String minioPublicEndpoint;

    public VideoContentBizService(VideoAssetMapper videoAssetMapper,
                                  TrainingPlanItemMapper trainingPlanItemMapper,
                                  MinioClient minioClient,
                                  MinioProperties minioProperties) {
        this.videoAssetMapper = videoAssetMapper;
        this.trainingPlanItemMapper = trainingPlanItemMapper;
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    public List<VideoAsset> list(Integer status, String keyword) {
        LambdaQueryWrapper<VideoAsset> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(VideoAsset::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(VideoAsset::getTitle, keyword).or().like(VideoAsset::getTags, keyword));
        }
        wrapper.orderByDesc(VideoAsset::getId);
        return videoAssetMapper.selectList(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(VideoAssetUpsertDTO dto) {
        VideoAsset asset = buildAsset(null, dto);
        videoAssetMapper.insert(asset);
        return asset.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean update(Long id, VideoAssetUpsertDTO dto) {
        VideoAsset exists = videoAssetMapper.selectById(id);
        if (exists == null) {
            return false;
        }
        VideoAsset asset = buildAsset(id, dto);
        return videoAssetMapper.updateById(asset) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        VideoAsset exists = videoAssetMapper.selectById(id);
        if (exists == null) {
            return false;
        }
        exists.setStatus(status);
        return videoAssetMapper.updateById(exists) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean bindToPlanItem(Long planItemId, Long videoId) {
        TrainingPlanItem item = trainingPlanItemMapper.selectById(planItemId);
        if (item == null) {
            return false;
        }
        VideoAsset asset = videoAssetMapper.selectById(videoId);
        if (asset == null || asset.getStatus() == null || asset.getStatus() != 1) {
            return false;
        }
        item.setVideoId(videoId);
        return trainingPlanItemMapper.updateById(item) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean unbindFromPlanItem(Long planItemId) {
        TrainingPlanItem item = trainingPlanItemMapper.selectById(planItemId);
        if (item == null) {
            return false;
        }
        item.setVideoId(null);
        return trainingPlanItemMapper.updateById(item) > 0;
    }

    private VideoAsset buildAsset(Long id, VideoAssetUpsertDTO dto) {
        VideoAsset asset = new VideoAsset();
        asset.setId(id);
        asset.setTitle(dto.getTitle());
        asset.setSourceSite(dto.getSourceSite());
        asset.setSourceUrl(dto.getSourceUrl());
        asset.setLicenseType(dto.getLicenseType());
        asset.setAttributionRequired(dto.getAttributionRequired());
        asset.setAuthorName(dto.getAuthorName());
        asset.setDurationSec(dto.getDurationSec() == null ? 0 : dto.getDurationSec());
        asset.setTags(dto.getTags());
        asset.setMinioPath(dto.getMinioPath());
        asset.setStatus(dto.getStatus());
        return asset;
    }

    public Map<String, Object> uploadVideo(MultipartFile file) {
        validateUpload(file);

        String extension = getFileExtension(file.getOriginalFilename());
        String datePath = LocalDate.now().toString().replace("-", "");
        String objectPath = "videos/upload/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + extension;

        try {
            ensureBucketExists();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectPath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("objectPath", objectPath);
            result.put("previewUrl", buildPreviewUrl(objectPath));
            result.put("size", file.getSize());
            result.put("contentType", file.getContentType());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("video upload failed: " + e.getMessage(), e);
        }
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is empty");
        }
        long maxBytes = 200L * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("file size exceeds 200MB");
        }
        String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        if (!".mp4".equals(extension) && !".mov".equals(extension) && !".webm".equals(extension)) {
            throw new IllegalArgumentException("only mp4/mov/webm are supported");
        }
    }

    private String getFileExtension(String originalName) {
        if (!StringUtils.hasText(originalName) || !originalName.contains(".")) {
            return ".mp4";
        }
        return originalName.substring(originalName.lastIndexOf("."));
    }

    private void ensureBucketExists() throws Exception {
        String bucket = minioProperties.getBucketName();
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private String buildPreviewUrl(String objectPath) throws Exception {
        return rewritePublicUrl(minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(minioProperties.getBucketName())
                        .object(objectPath)
                        .expiry(2, TimeUnit.HOURS)
                        .build()
        ));
    }

    private String rewritePublicUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        String internalEndpoint = StringUtils.hasText(minioProperties.getEndpoint())
                ? minioProperties.getEndpoint()
                : "http://localhost:9000";
        String publicEndpoint = StringUtils.hasText(minioPublicEndpoint)
                ? minioPublicEndpoint
                : "http://files.localhost";
        String safeInternal = internalEndpoint.endsWith("/")
                ? internalEndpoint.substring(0, internalEndpoint.length() - 1)
                : internalEndpoint;
        String safePublic = publicEndpoint.endsWith("/")
                ? publicEndpoint.substring(0, publicEndpoint.length() - 1)
                : publicEndpoint;
        return url.replace(safeInternal, safePublic).replace("http://localhost:9000", safePublic);
    }
}
