package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.constant.RedisConstant;
import com.huang.common.redis.MultiLevelCacheSupport;
import com.huang.model.entity.Banner;
import com.huang.web.admin.dto.banner.BannerUpsertDTO;
import com.huang.web.admin.mapper.BannerMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminBannerBizService {

    private final BannerMapper bannerMapper;
    private final MultiLevelCacheSupport multiLevelCacheSupport;

    public AdminBannerBizService(BannerMapper bannerMapper, MultiLevelCacheSupport multiLevelCacheSupport) {
        this.bannerMapper = bannerMapper;
        this.multiLevelCacheSupport = multiLevelCacheSupport;
    }

    public List<Banner> list(Integer status) {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<Banner>()
                .orderByAsc(Banner::getSort)
                .orderByDesc(Banner::getId);
        if (status != null) {
            wrapper.eq(Banner::getStatus, status);
        }
        return bannerMapper.selectList(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(BannerUpsertDTO dto) {
        Banner banner = new Banner();
        fill(banner, dto);
        bannerMapper.insert(banner);
        clearBannerCache();
        return banner.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean update(Long id, BannerUpsertDTO dto) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            return false;
        }
        fill(banner, dto);
        boolean updated = bannerMapper.updateById(banner) > 0;
        if (updated) {
            clearBannerCache();
        }
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            return false;
        }
        banner.setStatus(status);
        boolean updated = bannerMapper.updateById(banner) > 0;
        if (updated) {
            clearBannerCache();
        }
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        boolean deleted = bannerMapper.deleteById(id) > 0;
        if (deleted) {
            clearBannerCache();
        }
        return deleted;
    }

    private void fill(Banner banner, BannerUpsertDTO dto) {
        banner.setTitle(dto.getTitle());
        banner.setImageUrl(dto.getImageUrl());
        banner.setLinkUrl(dto.getLinkUrl());
        banner.setSort(dto.getSort());
        banner.setStatus(dto.getStatus());
    }

    private void clearBannerCache() {
        multiLevelCacheSupport.sharedEvict(RedisConstant.APP_BANNER_ACTIVE_KEY);
    }
}
