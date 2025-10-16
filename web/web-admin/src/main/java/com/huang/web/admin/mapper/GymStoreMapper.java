package com.huang.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.model.entity.GymStore;
import com.huang.web.admin.vo.store.GymStoreDetailVO;
import com.huang.web.admin.vo.store.GymStoreListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 门店管理表 Mapper 接口
 * @author system
 * @since 2025-01-24
 */
@Mapper
public interface GymStoreMapper extends BaseMapper<GymStore> {

    /**
     * 分页查询门店列表(带教练和会员统计)
     */
    List<GymStoreListVO> selectStoreListPage(Page<GymStoreListVO> page, @Param("query") Map<String, Object> query);

    /**
     * 查询门店详情(带教练、会员、课程统计)
     */
    GymStoreDetailVO selectStoreDetailById(@Param("id") Long id);

    /**
     * 查询门店统计数据
     */
    Map<String, Object> selectStoreStatistics();

    /**
     * 查询指定门店的教练列表
     */
    List<Map<String, Object>> selectCoachesByStoreId(@Param("storeId") Long storeId);

    /**
     * 查询指定门店的课程排期列表
     */
    List<Map<String, Object>> selectCoursesByStoreId(@Param("storeId") Long storeId);
}
