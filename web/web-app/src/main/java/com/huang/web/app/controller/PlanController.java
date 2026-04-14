package com.huang.web.app.controller;

import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.common.result.Result;
import com.huang.web.app.dto.plan.AiPlanGenerateDTO;
import com.huang.web.app.dto.plan.PlanSubscribeDTO;
import com.huang.web.app.dto.plan.PlanUnsubscribeDTO;
import com.huang.web.app.service.biz.PlanBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "App Training Plan", description = "Training plan browsing and subscription")
@RestController
@RequestMapping("/app/plan")
public class PlanController {

    private final PlanBizService planBizService;

    public PlanController(PlanBizService planBizService) {
        this.planBizService = planBizService;
    }

    @Operation(summary = "Plan overview")
    @GetMapping("/overview")
    public Result<?> overview() {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return Result.fail("未登录");
        }
        return Result.ok(planBizService.getPlanOverview(loginUser.getUserId()));
    }

    @Operation(summary = "Plan list")
    @GetMapping("/list")
    public Result<?> list() {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        Long userId = loginUser == null ? null : loginUser.getUserId();
        return Result.ok(planBizService.listActivePlans(userId));
    }

    @Operation(summary = "Plan detail")
    @GetMapping("/{planId:\\d+}")
    public Result<?> detail(@PathVariable Long planId) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        Long userId = loginUser == null ? null : loginUser.getUserId();
        Map<String, Object> detail = planBizService.getPlanDetail(planId, userId);
        if (detail == null) {
            return Result.fail("计划不存在或不可见");
        }
        return Result.ok(detail);
    }

    @Operation(summary = "Subscribe plan")
    @PostMapping("/subscribe")
    public Result<?> subscribe(@Valid @RequestBody PlanSubscribeDTO dto) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return Result.fail("未登录");
        }
        boolean ok = planBizService.subscribe(loginUser.getUserId(), dto);
        return ok ? Result.ok("订阅成功") : Result.fail("订阅失败");
    }

    @Operation(summary = "Unsubscribe plan")
    @PostMapping("/unsubscribe")
    public Result<?> unsubscribe(@Valid @RequestBody PlanUnsubscribeDTO dto) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return Result.fail("未登录");
        }
        boolean ok = planBizService.unsubscribe(loginUser.getUserId(), dto.getPlanId());
        return ok ? Result.ok("已取消激活") : Result.fail("取消激活失败");
    }

    @Operation(summary = "AI generate plan")
    @PostMapping("/ai-generate")
    public Result<?> aiGenerate(@Valid @RequestBody AiPlanGenerateDTO dto) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return Result.fail("未登录");
        }
        return Result.ok(planBizService.generateAndSavePlanByAi(dto.getUserPrompt()));
    }
}
