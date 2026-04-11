package com.huang.web.app.service.biz.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AppAuthSnapshot(Integer status, Integer tokenVersion) {
}
