package com.huang.web.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.model.entity.Coach;
import com.huang.web.app.mapper.CoachMapper;
import com.huang.web.app.service.CoachService;
import org.springframework.stereotype.Service;

/**
 * Coach服务实现类
 * @author system
 * @since 2025-01-24
 */
@Service
public class CoachServiceImpl extends ServiceImpl<CoachMapper, Coach> implements CoachService {

}
