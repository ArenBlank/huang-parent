package com.huang.common.redis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CacheInvalidationMessage {

    private CacheInvalidationAction action;
    private String target;
    private String sourceInstanceId;
    private long publishedAt;
}
