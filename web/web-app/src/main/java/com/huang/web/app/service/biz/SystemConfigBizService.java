package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.model.entity.SystemConfig;
import com.huang.web.app.mapper.SystemConfigMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemConfigBizService {

    private final SystemConfigMapper systemConfigMapper;

    public SystemConfigBizService(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }

    public Map<String, String> mapByKeys(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Map.of();
        }
        List<SystemConfig> rows = systemConfigMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
                .in(SystemConfig::getConfigKey, keys)
                .orderByDesc(SystemConfig::getId));
        Map<String, String> result = new HashMap<>();
        for (SystemConfig row : rows) {
            result.putIfAbsent(row.getConfigKey(), row.getConfigValue());
        }
        return result;
    }
}

