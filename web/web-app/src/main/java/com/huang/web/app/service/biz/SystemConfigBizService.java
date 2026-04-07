package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.constant.RedisConstant;
import com.huang.common.redis.RedisCacheSupport;
import com.huang.model.entity.SystemConfig;
import com.huang.web.app.mapper.SystemConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SystemConfigBizService {

    private final SystemConfigMapper systemConfigMapper;
    private final RedisCacheSupport redisCacheSupport;

    public SystemConfigBizService(SystemConfigMapper systemConfigMapper, RedisCacheSupport redisCacheSupport) {
        this.systemConfigMapper = systemConfigMapper;
        this.redisCacheSupport = redisCacheSupport;
    }

    public Map<String, String> mapByKeys(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Map.of();
        }

        List<String> orderedKeys = keys.stream()
                .filter(key -> key != null && !key.isBlank())
                .distinct()
                .toList();
        if (orderedKeys.isEmpty()) {
            return Map.of();
        }

        List<String> redisKeys = orderedKeys.stream()
                .map(RedisConstant::appSysConfigKey)
                .toList();
        List<String> cachedValues = redisCacheSupport.multiGet(redisKeys);

        Map<String, String> result = new LinkedHashMap<>();
        List<String> missingKeys = new ArrayList<>();
        for (int i = 0; i < orderedKeys.size(); i++) {
            String raw = i < cachedValues.size() ? cachedValues.get(i) : null;
            if (raw == null) {
                missingKeys.add(orderedKeys.get(i));
                continue;
            }
            if (!RedisConstant.CACHE_NULL_VALUE.equals(raw)) {
                result.put(orderedKeys.get(i), raw);
            }
        }

        if (CollectionUtils.isEmpty(missingKeys)) {
            return result;
        }

        List<SystemConfig> rows = systemConfigMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
                .in(SystemConfig::getConfigKey, missingKeys)
                .orderByDesc(SystemConfig::getId));
        Map<String, String> dbResult = new HashMap<>();
        for (SystemConfig row : rows) {
            dbResult.putIfAbsent(row.getConfigKey(), row.getConfigValue());
        }
        for (String key : missingKeys) {
            String configValue = dbResult.get(key);
            if (configValue == null) {
                redisCacheSupport.cacheNull(RedisConstant.appSysConfigKey(key), RedisConstant.CACHE_NULL_TTL_SEC);
            } else {
                redisCacheSupport.setString(
                        RedisConstant.appSysConfigKey(key),
                        configValue,
                        redisCacheSupport.ttlWithJitter(RedisConstant.APP_SYS_CONFIG_TTL_SEC, RedisConstant.JITTER_LONG_SEC)
                );
                result.put(key, configValue);
            }
        }
        return orderedKeys.stream()
                .filter(result::containsKey)
                .collect(Collectors.toMap(key -> key, result::get, (left, right) -> left, LinkedHashMap::new));
    }
}
