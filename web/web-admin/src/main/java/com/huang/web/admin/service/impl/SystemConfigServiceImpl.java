package com.huang.web.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.model.entity.SystemConfig;
import com.huang.web.admin.mapper.SystemConfigMapper;
import com.huang.web.admin.service.SystemConfigService;
import org.springframework.stereotype.Service;

/**
 * SystemConfig服务实现类
 * @author system
 * @since 2026-02-25
 */
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

}
