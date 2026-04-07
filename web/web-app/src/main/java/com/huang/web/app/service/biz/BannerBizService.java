package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.huang.common.constant.RedisConstant;
import com.huang.common.redis.RedisCacheSupport;
import com.huang.model.entity.Banner;
import com.huang.web.app.mapper.BannerMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BannerBizService {

    private final BannerMapper bannerMapper;
    private final RedisCacheSupport redisCacheSupport;

    public BannerBizService(BannerMapper bannerMapper, RedisCacheSupport redisCacheSupport) {
        this.bannerMapper = bannerMapper;
        this.redisCacheSupport = redisCacheSupport;
    }

    public List<Banner> listActive() {
        var cached = redisCacheSupport.getJson(
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
        redisCacheSupport.setJson(
                RedisConstant.APP_BANNER_ACTIVE_KEY,
                banners,
                redisCacheSupport.ttlWithJitter(RedisConstant.APP_BANNER_TTL_SEC, RedisConstant.JITTER_SHORT_SEC)
        );
        return banners;
    }
}
