package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.model.entity.Banner;
import com.huang.web.app.mapper.BannerMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BannerBizService {

    private final BannerMapper bannerMapper;

    public BannerBizService(BannerMapper bannerMapper) {
        this.bannerMapper = bannerMapper;
    }

    public List<Banner> listActive() {
        return bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getStatus, 1)
                .orderByAsc(Banner::getSort)
                .orderByDesc(Banner::getId));
    }
}

