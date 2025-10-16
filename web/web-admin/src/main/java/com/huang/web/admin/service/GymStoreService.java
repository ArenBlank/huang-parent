package com.huang.web.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.huang.model.entity.GymStore;
import com.huang.web.admin.dto.store.GymStoreAddDTO;
import com.huang.web.admin.dto.store.GymStoreQueryDTO;
import com.huang.web.admin.dto.store.GymStoreUpdateDTO;
import com.huang.web.admin.vo.store.GymStoreDetailVO;
import com.huang.web.admin.vo.store.GymStoreListVO;
import com.huang.web.admin.vo.store.GymStoreStatisticsVO;

import java.util.List;
import java.util.Map;

/**
 * 门店管理服务类
 * @author system
 * @since 2025-01-24
 */
public interface GymStoreService extends IService<GymStore> {

    /**
     * 分页查询门店列表
     */
    Page<GymStoreListVO> getStoreListPage(GymStoreQueryDTO queryDTO);

    /**
     * 查询门店详情
     */
    GymStoreDetailVO getStoreDetail(Long id);

    /**
     * 新增门店
     */
    Long addStore(GymStoreAddDTO addDTO);

    /**
     * 更新门店信息
     */
    boolean updateStore(GymStoreUpdateDTO updateDTO);

    /**
     * 删除门店(逻辑删除)
     */
    boolean deleteStore(Long id);

    /**
     * 批量删除门店
     */
    boolean deleteBatch(List<Long> ids);

    /**
     * 修改门店状态
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 查询门店统计数据
     */
    GymStoreStatisticsVO getStoreStatistics();

    /**
     * 查询指定门店的教练列表
     */
    List<Map<String, Object>> getStoreCoaches(Long storeId);

    /**
     * 查询指定门店的课程列表
     */
    List<Map<String, Object>> getStoreCourses(Long storeId);
}
