package com.huang.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.model.entity.GymStore;
import com.huang.web.app.vo.store.GymStoreDetailVO;
import com.huang.web.app.vo.store.GymStoreListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * App端门店管理表 Mapper 接口
 * @author system
 * @since 2025-01-24
 */
@Mapper
public interface AppGymStoreMapper extends BaseMapper<GymStore> {

    /**
     * 分页查询门店列表(仅营业中的门店)
     */
    List<GymStoreListVO> selectStoreListPage(Page<GymStoreListVO> page, @Param("query") Map<String, Object> query);

    /**
     * 查询门店详情(带教练和今日课程统计)
     */
    GymStoreDetailVO selectStoreDetailById(@Param("id") Long id, @Param("userLatitude") Double userLatitude, @Param("userLongitude") Double userLongitude);

    /**
     * 查询附近门店列表(基于经纬度计算距离)
     */
    List<GymStoreListVO> selectNearbyStores(@Param("userLatitude") Double userLatitude,
                                           @Param("userLongitude") Double userLongitude,
                                           @Param("radius") Integer radius);

    /**
     * 查询指定门店的教练列表
     */
    List<Map<String, Object>> selectCoachesByStoreId(@Param("storeId") Long storeId);

    /**
     * 查询指定门店的课程排期(今日及未来)
     */
    List<Map<String, Object>> selectCoursesByStoreId(@Param("storeId") Long storeId);
}
