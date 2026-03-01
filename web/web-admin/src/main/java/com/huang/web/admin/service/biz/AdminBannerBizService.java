package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.model.entity.Banner;
import com.huang.web.admin.dto.banner.BannerUpsertDTO;
import com.huang.web.admin.mapper.BannerMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminBannerBizService {

    private final BannerMapper bannerMapper;

    public AdminBannerBizService(BannerMapper bannerMapper) {
        this.bannerMapper = bannerMapper;
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
        return banner.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean update(Long id, BannerUpsertDTO dto) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            return false;
        }
        fill(banner, dto);
        return bannerMapper.updateById(banner) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            return false;
        }
        banner.setStatus(status);
        return bannerMapper.updateById(banner) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        return bannerMapper.deleteById(id) > 0;
    }

    private void fill(Banner banner, BannerUpsertDTO dto) {
        banner.setTitle(dto.getTitle());
        banner.setImageUrl(dto.getImageUrl());
        banner.setLinkUrl(dto.getLinkUrl());
        banner.setSort(dto.getSort());
        banner.setStatus(dto.getStatus());
    }
}

