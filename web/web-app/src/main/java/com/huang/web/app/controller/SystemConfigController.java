package com.huang.web.app.controller;

import com.huang.common.result.Result;
import com.huang.web.app.service.biz.SystemConfigBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "App系统配置", description = "读取前端需要的系统参数")
@RestController
@RequestMapping("/app/system-config")
public class SystemConfigController {

    private final SystemConfigBizService systemConfigBizService;

    public SystemConfigController(SystemConfigBizService systemConfigBizService) {
        this.systemConfigBizService = systemConfigBizService;
    }

    @Operation(summary = "按key批量读取配置")
    @GetMapping("/map")
    public Result<?> mapByKeys(@RequestParam List<String> keys) {
        return Result.ok(systemConfigBizService.mapByKeys(keys));
    }
}

