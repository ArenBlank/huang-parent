package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.huang.common.constant.RedisConstant;
import com.huang.common.redis.RedisCacheSupport;
import com.huang.model.entity.Notice;
import com.huang.web.app.mapper.NoticeMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoticeBizService {

    private final NoticeMapper noticeMapper;
    private final RedisCacheSupport redisCacheSupport;

    public NoticeBizService(NoticeMapper noticeMapper, RedisCacheSupport redisCacheSupport) {
        this.noticeMapper = noticeMapper;
        this.redisCacheSupport = redisCacheSupport;
    }

    public List<Notice> listPublished(Integer limit) {
        int safeLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);
        String cacheKey = RedisConstant.appNoticePublishedKey(safeLimit);
        var cached = redisCacheSupport.getJson(cacheKey, new TypeReference<List<Notice>>() {});
        if (cached.found()) {
            return cached.nullValue() ? List.of() : cached.value();
        }

        List<Notice> notices = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, 1)
                .le(Notice::getPublishTime, LocalDateTime.now())
                .orderByDesc(Notice::getPublishTime)
                .last("LIMIT " + safeLimit));
        redisCacheSupport.setJson(
                cacheKey,
                notices,
                redisCacheSupport.ttlWithJitter(RedisConstant.APP_NOTICE_TTL_SEC, RedisConstant.JITTER_SHORT_SEC)
        );
        return notices;
    }
}
