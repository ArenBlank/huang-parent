package com.huang.web.app.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huang.model.entity.GymStore;
import com.huang.web.app.dto.store.GymStoreQueryDTO;
import com.huang.web.app.mapper.AppGymStoreMapper;
import com.huang.web.app.service.AppGymStoreService;
import com.huang.web.app.vo.store.GymStoreDetailVO;
import com.huang.web.app.vo.store.GymStoreListVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * App端门店管理服务实现类
 * @author system
 * @since 2025-01-24
 */
@Slf4j
@Service
public class AppGymStoreServiceImpl extends ServiceImpl<AppGymStoreMapper, GymStore> implements AppGymStoreService {

    @Autowired
    private AppGymStoreMapper appGymStoreMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Page<GymStoreListVO> getStoreListPage(GymStoreQueryDTO queryDTO) {
        Page<GymStoreListVO> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        // 构建查询条件
        Map<String, Object> query = new HashMap<>();
        query.put("storeName", queryDTO.getStoreName());
        query.put("address", queryDTO.getAddress());

        List<GymStoreListVO> records = appGymStoreMapper.selectStoreListPage(page, query);

        // 如果用户提供了位置信息,计算距离
        if (queryDTO.getUserLatitude() != null && queryDTO.getUserLongitude() != null) {
            for (GymStoreListVO vo : records) {
                if (vo.getLatitude() != null && vo.getLongitude() != null) {
                    double distance = calculateDistance(
                            queryDTO.getUserLatitude().doubleValue(),
                            queryDTO.getUserLongitude().doubleValue(),
                            vo.getLatitude().doubleValue(),
                            vo.getLongitude().doubleValue()
                    );
                    vo.setDistance(distance);
                }
            }
        }

        page.setRecords(records);
        return page;
    }

    @Override
    public GymStoreDetailVO getStoreDetail(Long id, Double userLatitude, Double userLongitude) {
        GymStoreDetailVO detailVO = appGymStoreMapper.selectStoreDetailById(id, userLatitude, userLongitude);
        if (detailVO == null) {
            throw new RuntimeException("门店不存在或已停业");
        }

        // 解析JSON字段
        try {
            // 解析设施信息
            if (StringUtils.hasText(detailVO.getFacilitiesJson())) {
                Object facilities = objectMapper.readValue(
                        detailVO.getFacilitiesJson(),
                        new TypeReference<Map<String, Object>>() {}
                );
                detailVO.setFacilities(facilities);
            }

            // 解析图片列表
            if (StringUtils.hasText(detailVO.getImages())) {
                List<String> imageList = objectMapper.readValue(
                        detailVO.getImages(),
                        new TypeReference<List<String>>() {}
                );
                detailVO.setImageList(imageList);
            }
        } catch (Exception e) {
            log.error("解析JSON字段失败", e);
        }

        return detailVO;
    }

    @Override
    public List<GymStoreListVO> getNearbyStores(Double userLatitude, Double userLongitude, Integer radius) {
        if (userLatitude == null || userLongitude == null) {
            throw new RuntimeException("请提供您的位置信息");
        }

        // 默认搜索半径5公里
        if (radius == null || radius <= 0) {
            radius = 5;
        }

        // 限制最大搜索半径为50公里
        if (radius > 50) {
            radius = 50;
        }

        List<GymStoreListVO> stores = appGymStoreMapper.selectNearbyStores(userLatitude, userLongitude, radius);

        log.info("查询附近门店, 位置: ({}, {}), 半径: {}km, 结果数量: {}",
                userLatitude, userLongitude, radius, stores.size());

        return stores;
    }

    @Override
    public Page<GymStoreListVO> searchStores(String keyword, Long current, Long size) {
        Page<GymStoreListVO> page = new Page<>(current, size);

        // 构建查询条件(同时搜索名称和地址)
        Map<String, Object> query = new HashMap<>();
        if (StringUtils.hasText(keyword)) {
            query.put("storeName", keyword);
            query.put("address", keyword);
        }

        List<GymStoreListVO> records = appGymStoreMapper.selectStoreListPage(page, query);
        page.setRecords(records);

        return page;
    }

    @Override
    public List<Map<String, Object>> getStoreCoaches(Long storeId) {
        // 检查门店是否存在且营业
        GymStore existStore = baseMapper.selectById(storeId);
        if (existStore == null || existStore.getIsDeleted() == (byte) 1 || existStore.getStatus() != 1) {
            throw new RuntimeException("门店不存在或已停业");
        }

        return appGymStoreMapper.selectCoachesByStoreId(storeId);
    }

    @Override
    public List<Map<String, Object>> getStoreCourses(Long storeId) {
        // 检查门店是否存在且营业
        GymStore existStore = baseMapper.selectById(storeId);
        if (existStore == null || existStore.getIsDeleted() == (byte) 1 || existStore.getStatus() != 1) {
            throw new RuntimeException("门店不存在或已停业");
        }

        return appGymStoreMapper.selectCoursesByStoreId(storeId);
    }

    /**
     * 计算两点之间的距离(Haversine公式)
     * @param lat1 纬度1
     * @param lon1 经度1
     * @param lat2 纬度2
     * @param lon2 经度2
     * @return 距离(公里)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371; // 地球半径(公里)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = EARTH_RADIUS * c;

        // 保留两位小数
        return Math.round(distance * 100.0) / 100.0;
    }
}
