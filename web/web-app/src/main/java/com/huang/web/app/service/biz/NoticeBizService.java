package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.huang.common.constant.RedisConstant;
import com.huang.common.redis.MultiLevelCacheSupport;
import com.huang.model.entity.Notice;
import com.huang.web.app.mapper.NoticeMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoticeBizService {

    private final NoticeMapper noticeMapper;
    private final MultiLevelCacheSupport multiLevelCacheSupport;

    public NoticeBizService(NoticeMapper noticeMapper, MultiLevelCacheSupport multiLevelCacheSupport) {
        this.noticeMapper = noticeMapper;
        this.multiLevelCacheSupport = multiLevelCacheSupport;
    }

    public List<Notice> listPublished(Integer limit) {
        int safeLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);
        String cacheKey = RedisConstant.appNoticePublishedKey(safeLimit);
        var cached = multiLevelCacheSupport.getJson(cacheKey, new TypeReference<List<Notice>>() {});
        if (cached.found()) {
            return cached.nullValue() ? List.of() : cached.value();
        }

        List<Notice> notices = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, 1)
                .le(Notice::getPublishTime, LocalDateTime.now())
                .orderByDesc(Notice::getPublishTime)
                .last("LIMIT " + safeLimit));
        multiLevelCacheSupport.setJson(
                cacheKey,
                notices,
                multiLevelCacheSupport.ttlWithJitter(RedisConstant.APP_NOTICE_TTL_SEC, RedisConstant.JITTER_SHORT_SEC)
        );
        return notices;
    }
}
