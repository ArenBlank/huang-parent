package com.huang.web.app.controller;

import com.huang.common.result.Result;
import com.huang.web.app.dto.coach.CoachApplyDTO;
import com.huang.web.app.service.biz.CoachApplyBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "App教练申请")
@RestController
@RequestMapping("/app/coach")
public class CoachApplyController {

    private final CoachApplyBizService coachApplyBizService;

    public CoachApplyController(CoachApplyBizService coachApplyBizService) {
        this.coachApplyBizService = coachApplyBizService;
    }

    @Operation(summary = "提交教练申请")
    @PostMapping("/apply")
    public Result<?> apply(@Valid @RequestBody CoachApplyDTO dto) {
        return Result.ok(coachApplyBizService.apply(dto));
    }

    @Operation(summary = "我的教练申请")
    @GetMapping("/my-application")
    public Result<?> myApplication() {
        return Result.ok(coachApplyBizService.myApply());
    }
}

