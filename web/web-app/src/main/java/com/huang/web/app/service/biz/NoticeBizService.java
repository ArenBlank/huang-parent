package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.model.entity.Notice;
import com.huang.web.app.mapper.NoticeMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoticeBizService {

    private final NoticeMapper noticeMapper;

    public NoticeBizService(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    public List<Notice> listPublished(Integer limit) {
        int safeLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);
        return noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, 1)
                .le(Notice::getPublishTime, LocalDateTime.now())
                .orderByDesc(Notice::getPublishTime)
                .last("LIMIT " + safeLimit));
    }
}

