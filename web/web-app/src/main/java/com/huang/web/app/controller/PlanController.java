package com.huang.web.app.controller;

import com.huang.common.result.Result;
import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.web.app.dto.plan.PlanSubscribeDTO;
import com.huang.web.app.service.biz.PlanBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "App训练计划", description = "训练计划与订阅")
@RestController
@RequestMapping("/app/plan")
public class PlanController {

    private final PlanBizService planBizService;

    public PlanController(PlanBizService planBizService) {
        this.planBizService = planBizService;
    }

    @Operation(summary = "计划列表")
    @GetMapping("/list")
    public Result<?> list() {
        return Result.ok(planBizService.listActivePlans());
    }

    @Operation(summary = "计划详情")
    @GetMapping("/{planId}")
    public Result<?> detail(@PathVariable Long planId) {
        Map<String, Object> detail = planBizService.getPlanDetail(planId);
        if (detail == null) {
            return Result.fail("计划不存在或不可用");
        }
        return Result.ok(detail);
    }

    @Operation(summary = "订阅计划")
    @PostMapping("/subscribe")
    public Result<?> subscribe(@Valid @RequestBody PlanSubscribeDTO dto) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return Result.fail("未登录");
        }
        boolean ok = planBizService.subscribe(loginUser.getUserId(), dto);
        return ok ? Result.ok("订阅成功") : Result.fail("订阅失败");
    }
}
