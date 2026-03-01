package com.huang.web.app.controller;

import com.huang.common.result.Result;
import com.huang.web.app.service.biz.NoticeBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "App运营内容", description = "公告与运营位")
@RestController
@RequestMapping("/app/notice")
public class NoticeController {

    private final NoticeBizService noticeBizService;

    public NoticeController(NoticeBizService noticeBizService) {
        this.noticeBizService = noticeBizService;
    }

    @Operation(summary = "公告列表")
    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(noticeBizService.listPublished(limit));
    }
}

