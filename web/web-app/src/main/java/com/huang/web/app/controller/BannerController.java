package com.huang.web.app.controller;

import com.huang.common.result.Result;
import com.huang.web.app.service.biz.BannerBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "App运营内容", description = "首页运营位")
@RestController
@RequestMapping("/app/banner")
public class BannerController {

    private final BannerBizService bannerBizService;

    public BannerController(BannerBizService bannerBizService) {
        this.bannerBizService = bannerBizService;
    }

    @Operation(summary = "首页Banner列表")
    @GetMapping("/list")
    public Result<?> list() {
        return Result.ok(bannerBizService.listActive());
    }
}

