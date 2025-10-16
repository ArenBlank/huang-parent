package com.huang.web.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.model.entity.GymStore;
import com.huang.web.admin.dto.store.GymStoreAddDTO;
import com.huang.web.admin.dto.store.GymStoreQueryDTO;
import com.huang.web.admin.dto.store.GymStoreUpdateDTO;
import com.huang.web.admin.mapper.GymStoreMapper;
import com.huang.web.admin.service.GymStoreService;
import com.huang.web.admin.vo.store.GymStoreDetailVO;
import com.huang.web.admin.vo.store.GymStoreListVO;
import com.huang.web.admin.vo.store.GymStoreStatisticsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 门店管理服务实现类
 * @author system
 * @since 2025-01-24
 */
@Slf4j
@Service
public class GymStoreServiceImpl extends ServiceImpl<GymStoreMapper, GymStore> implements GymStoreService {

    @Autowired
    private GymStoreMapper gymStoreMapper;

    @Override
    public Page<GymStoreListVO> getStoreListPage(GymStoreQueryDTO queryDTO) {
        Page<GymStoreListVO> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        // 构建查询条件
        Map<String, Object> query = new HashMap<>();
        query.put("storeName", queryDTO.getStoreName());
        query.put("storeCode", queryDTO.getStoreCode());
        query.put("address", queryDTO.getAddress());
        query.put("status", queryDTO.getStatus());
        query.put("managerName", queryDTO.getManagerName());
        query.put("phone", queryDTO.getPhone());

        List<GymStoreListVO> records = gymStoreMapper.selectStoreListPage(page, query);
        page.setRecords(records);

        return page;
    }

    @Override
    public GymStoreDetailVO getStoreDetail(Long id) {
        GymStoreDetailVO detailVO = gymStoreMapper.selectStoreDetailById(id);
        if (detailVO == null) {
            throw new RuntimeException("门店不存在");
        }
        return detailVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addStore(GymStoreAddDTO addDTO) {
        // 检查门店编码是否已存在
        LambdaQueryWrapper<GymStore> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GymStore::getStoreCode, addDTO.getStoreCode())
                .eq(GymStore::getIsDeleted, (byte) 0);
        Long count = baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new RuntimeException("门店编码已存在");
        }

        // 实体转换
        GymStore gymStore = new GymStore();
        BeanUtils.copyProperties(addDTO, gymStore);
        gymStore.setCreateTime(LocalDateTime.now());
        gymStore.setUpdateTime(LocalDateTime.now());
        gymStore.setIsDeleted((byte) 0);

        // 保存
        baseMapper.insert(gymStore);
        log.info("新增门店成功, ID: {}, 门店名称: {}", gymStore.getId(), gymStore.getStoreName());

        return gymStore.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStore(GymStoreUpdateDTO updateDTO) {
        // 检查门店是否存在
        GymStore existStore = baseMapper.selectById(updateDTO.getId());
        if (existStore == null || existStore.getIsDeleted() == (byte) 1) {
            throw new RuntimeException("门店不存在");
        }

        // 检查门店编码是否与其他门店重复
        LambdaQueryWrapper<GymStore> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GymStore::getStoreCode, updateDTO.getStoreCode())
                .eq(GymStore::getIsDeleted, (byte) 0)
                .ne(GymStore::getId, updateDTO.getId());
        Long count = baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new RuntimeException("门店编码已被其他门店使用");
        }

        // 实体转换
        GymStore gymStore = new GymStore();
        BeanUtils.copyProperties(updateDTO, gymStore);
        gymStore.setUpdateTime(LocalDateTime.now());

        // 更新
        int result = baseMapper.updateById(gymStore);
        log.info("更新门店成功, ID: {}, 门店名称: {}", gymStore.getId(), gymStore.getStoreName());

        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteStore(Long id) {
        // 检查门店是否存在
        GymStore existStore = baseMapper.selectById(id);
        if (existStore == null || existStore.getIsDeleted() == (byte) 1) {
            throw new RuntimeException("门店不存在");
        }

        // 逻辑删除
        LambdaUpdateWrapper<GymStore> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(GymStore::getId, id)
                .set(GymStore::getIsDeleted, (byte) 1)
                .set(GymStore::getUpdateTime, LocalDateTime.now());

        boolean result = update(updateWrapper);
        log.info("删除门店成功, ID: {}", id);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }

        // 批量逻辑删除
        LambdaUpdateWrapper<GymStore> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(GymStore::getId, ids)
                .set(GymStore::getIsDeleted, (byte) 1)
                .set(GymStore::getUpdateTime, LocalDateTime.now());

        boolean result = update(updateWrapper);
        log.info("批量删除门店成功, 数量: {}", ids.size());

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        // 检查门店是否存在
        GymStore existStore = baseMapper.selectById(id);
        if (existStore == null || existStore.getIsDeleted() == (byte) 1) {
            throw new RuntimeException("门店不存在");
        }

        // 更新状态
        LambdaUpdateWrapper<GymStore> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(GymStore::getId, id)
                .set(GymStore::getStatus, status)
                .set(GymStore::getUpdateTime, LocalDateTime.now());

        boolean result = update(updateWrapper);
        log.info("更新门店状态成功, ID: {}, 状态: {}", id, status);

        return result;
    }

    @Override
    public GymStoreStatisticsVO getStoreStatistics() {
        Map<String, Object> stats = gymStoreMapper.selectStoreStatistics();

        GymStoreStatisticsVO statisticsVO = new GymStoreStatisticsVO();
        statisticsVO.setTotalStores(((Number) stats.get("total_stores")).intValue());
        statisticsVO.setActiveStores(((Number) stats.get("active_stores")).intValue());
        statisticsVO.setClosedStores(((Number) stats.get("closed_stores")).intValue());
        statisticsVO.setRenovatingStores(((Number) stats.get("renovating_stores")).intValue());
        statisticsVO.setTotalCoaches(((Number) stats.get("total_coaches")).intValue());
        statisticsVO.setTotalMembers(((Number) stats.get("total_members")).intValue());
        statisticsVO.setTotalCourses(((Number) stats.get("total_courses")).intValue());

        return statisticsVO;
    }

    @Override
    public List<Map<String, Object>> getStoreCoaches(Long storeId) {
        // 检查门店是否存在
        GymStore existStore = baseMapper.selectById(storeId);
        if (existStore == null || existStore.getIsDeleted() == (byte) 1) {
            throw new RuntimeException("门店不存在");
        }

        return gymStoreMapper.selectCoachesByStoreId(storeId);
    }

    @Override
    public List<Map<String, Object>> getStoreCourses(Long storeId) {
        // 检查门店是否存在
        GymStore existStore = baseMapper.selectById(storeId);
        if (existStore == null || existStore.getIsDeleted() == (byte) 1) {
            throw new RuntimeException("门店不存在");
        }

        return gymStoreMapper.selectCoursesByStoreId(storeId);
    }
}
