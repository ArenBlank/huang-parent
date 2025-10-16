package com.huang.web.app.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.huang.model.entity.GymStore;
import com.huang.web.app.dto.store.GymStoreQueryDTO;
import com.huang.web.app.vo.store.GymStoreDetailVO;
import com.huang.web.app.vo.store.GymStoreListVO;

import java.util.List;
import java.util.Map;

/**
 * App端门店管理服务类
 * @author system
 * @since 2025-01-24
 */
public interface AppGymStoreService extends IService<GymStore> {

    /**
     * 分页查询门店列表(仅营业中的门店)
     */
    Page<GymStoreListVO> getStoreListPage(GymStoreQueryDTO queryDTO);

    /**
     * 查询门店详情
     */
    GymStoreDetailVO getStoreDetail(Long id, Double userLatitude, Double userLongitude);

    /**
     * 查询附近门店列表
     */
    List<GymStoreListVO> getNearbyStores(Double userLatitude, Double userLongitude, Integer radius);

    /**
     * 搜索门店(按名称或地址)
     */
    Page<GymStoreListVO> searchStores(String keyword, Long current, Long size);

    /**
     * 查询指定门店的教练列表
     */
    List<Map<String, Object>> getStoreCoaches(Long storeId);

    /**
     * 查询指定门店的课程排期
     */
    List<Map<String, Object>> getStoreCourses(Long storeId);
}
