package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.constant.RedisConstant;
import com.huang.common.redis.RedisCacheSupport;
import com.huang.model.entity.SystemConfig;
import com.huang.web.admin.dto.config.SystemConfigUpsertDTO;
import com.huang.web.admin.mapper.SystemConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminSystemConfigBizService {

    private final SystemConfigMapper systemConfigMapper;
    private final RedisCacheSupport redisCacheSupport;

    public AdminSystemConfigBizService(SystemConfigMapper systemConfigMapper, RedisCacheSupport redisCacheSupport) {
        this.systemConfigMapper = systemConfigMapper;
        this.redisCacheSupport = redisCacheSupport;
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
        redisCacheSupport.safeDelete(RedisConstant.appSysConfigKey(config.getConfigKey()));
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
        String oldKey = exists.getConfigKey();
        fill(exists, dto);
        boolean updated = systemConfigMapper.updateById(exists) > 0;
        if (updated) {
            redisCacheSupport.safeDelete(RedisConstant.appSysConfigKey(oldKey));
            redisCacheSupport.safeDelete(RedisConstant.appSysConfigKey(exists.getConfigKey()));
        }
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        SystemConfig exists = systemConfigMapper.selectById(id);
        if (exists == null) {
            return false;
        }
        boolean deleted = systemConfigMapper.deleteById(id) > 0;
        if (deleted) {
            redisCacheSupport.safeDelete(RedisConstant.appSysConfigKey(exists.getConfigKey()));
        }
        return deleted;
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
