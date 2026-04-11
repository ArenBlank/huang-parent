package com.huang.common.redis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cache.invalidation")
public class CacheInvalidationProperties {

    private String channel = "cache:evict";
}
