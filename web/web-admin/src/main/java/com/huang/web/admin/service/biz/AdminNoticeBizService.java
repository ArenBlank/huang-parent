package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.constant.RedisConstant;
import com.huang.common.redis.RedisCacheSupport;
import com.huang.model.entity.Notice;
import com.huang.web.admin.dto.notice.NoticeUpsertDTO;
import com.huang.web.admin.mapper.NoticeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminNoticeBizService {

    private final NoticeMapper noticeMapper;
    private final RedisCacheSupport redisCacheSupport;

    public AdminNoticeBizService(NoticeMapper noticeMapper, RedisCacheSupport redisCacheSupport) {
        this.noticeMapper = noticeMapper;
        this.redisCacheSupport = redisCacheSupport;
    }

    public List<Notice> list(Integer status) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .orderByDesc(Notice::getPublishTime)
                .orderByDesc(Notice::getId);
        if (status != null) {
            wrapper.eq(Notice::getStatus, status);
        }
        return noticeMapper.selectList(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(NoticeUpsertDTO dto) {
        Notice notice = new Notice();
        fill(notice, dto);
        noticeMapper.insert(notice);
        clearNoticeCaches();
        return notice.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean update(Long id, NoticeUpsertDTO dto) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            return false;
        }
        fill(notice, dto);
        boolean updated = noticeMapper.updateById(notice) > 0;
        if (updated) {
            clearNoticeCaches();
        }
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            return false;
        }
        notice.setStatus(status);
        if (status == 1 && notice.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }
        boolean updated = noticeMapper.updateById(notice) > 0;
        if (updated) {
            clearNoticeCaches();
        }
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        boolean deleted = noticeMapper.deleteById(id) > 0;
        if (deleted) {
            clearNoticeCaches();
        }
        return deleted;
    }

    private void fill(Notice notice, NoticeUpsertDTO dto) {
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setStatus(dto.getStatus());
        if (dto.getPublishTime() != null) {
            notice.setPublishTime(dto.getPublishTime());
        } else if (dto.getStatus() != null && dto.getStatus() == 1 && notice.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }
    }

    private void clearNoticeCaches() {
        redisCacheSupport.deleteByPrefix(RedisConstant.APP_NOTICE_PUBLISHED_PREFIX);
    }
}
