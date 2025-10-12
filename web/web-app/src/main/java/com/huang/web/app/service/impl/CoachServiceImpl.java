package com.huang.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.model.entity.Coach;
import com.huang.web.app.mapper.CoachMapper;
import com.huang.web.app.service.CoachService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Coach服务实现类
 * @author huang
 * @since 2025-01-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoachServiceImpl extends ServiceImpl<CoachMapper, Coach> implements CoachService {

    @Override
    public Coach getCoachByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        
        LambdaQueryWrapper<Coach> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coach::getUserId, userId)
                .eq(Coach::getIsDeleted, 0)
                .last("LIMIT 1");
                
        return getOne(wrapper);
    }
}