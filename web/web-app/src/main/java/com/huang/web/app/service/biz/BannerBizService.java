package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.huang.common.constant.RedisConstant;
import com.huang.common.redis.MultiLevelCacheSupport;
import com.huang.model.entity.Banner;
import com.huang.web.app.mapper.BannerMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BannerBizService {

    private final BannerMapper bannerMapper;
    private final MultiLevelCacheSupport multiLevelCacheSupport;

    public BannerBizService(BannerMapper bannerMapper, MultiLevelCacheSupport multiLevelCacheSupport) {
        this.bannerMapper = bannerMapper;
        this.multiLevelCacheSupport = multiLevelCacheSupport;
    }

    public List<Banner> listActive() {
        var cached = multiLevelCacheSupport.getJson(
                RedisConstant.APP_BANNER_ACTIVE_KEY,
                new TypeReference<List<Banner>>() {}
        );
        if (cached.found()) {
            return cached.nullValue() ? List.of() : cached.value();
        }

        List<Banner> banners = bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getStatus, 1)
                .orderByAsc(Banner::getSort)
                .orderByDesc(Banner::getId));
        multiLevelCacheSupport.setJson(
                RedisConstant.APP_BANNER_ACTIVE_KEY,
                banners,
                multiLevelCacheSupport.ttlWithJitter(RedisConstant.APP_BANNER_TTL_SEC, RedisConstant.JITTER_SHORT_SEC)
        );
        return banners;
    }
}
