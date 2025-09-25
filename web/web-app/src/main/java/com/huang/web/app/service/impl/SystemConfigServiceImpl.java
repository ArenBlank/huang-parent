package com.huang.web.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.model.entity.SystemConfig;
import com.huang.web.app.mapper.SystemConfigMapper;
import com.huang.web.app.service.SystemConfigService;
import org.springframework.stereotype.Service;

/**
 * SystemConfig服务实现类
 * @author system
 * @since 2025-01-24
 */
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

}
