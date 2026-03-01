package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.model.entity.SystemConfig;
import com.huang.web.admin.dto.config.SystemConfigUpsertDTO;
import com.huang.web.admin.mapper.SystemConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminSystemConfigBizService {

    private final SystemConfigMapper systemConfigMapper;

    public AdminSystemConfigBizService(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }

    public List<SystemConfig> list(String keyLike) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<SystemConfig>()
                .orderByDesc(SystemConfig::getId);
        if (keyLike != null && !keyLike.isBlank()) {
            wrapper.like(SystemConfig::getConfigKey, keyLike);
        }
        return systemConfigMapper.selectList(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(SystemConfigUpsertDTO dto) {
        if (existsByKey(dto.getConfigKey(), null)) {
            return null;
        }
        SystemConfig config = new SystemConfig();
        fill(config, dto);
        systemConfigMapper.insert(config);
        return config.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean update(Long id, SystemConfigUpsertDTO dto) {
        SystemConfig exists = systemConfigMapper.selectById(id);
        if (exists == null) {
            return false;
        }
        if (existsByKey(dto.getConfigKey(), id)) {
            return false;
        }
        fill(exists, dto);
        return systemConfigMapper.updateById(exists) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        return systemConfigMapper.deleteById(id) > 0;
    }

    private boolean existsByKey(String key, Long excludeId) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key)
                .last("LIMIT 1");
        if (excludeId != null) {
            wrapper.ne(SystemConfig::getId, excludeId);
        }
        return systemConfigMapper.selectOne(wrapper) != null;
    }

    private void fill(SystemConfig config, SystemConfigUpsertDTO dto) {
        config.setConfigKey(dto.getConfigKey());
        config.setConfigValue(dto.getConfigValue());
        config.setRemark(dto.getRemark());
    }
}

