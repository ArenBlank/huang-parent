package com.huang.web.app.controller;

import com.huang.common.result.Result;
import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.web.app.dto.record.TrainingCheckinDTO;
import com.huang.web.app.service.biz.TrainingRecordBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "App训练打卡", description = "训练打卡与统计")
@RestController
@RequestMapping("/app/record")
public class TrainingRecordController {

    private final TrainingRecordBizService trainingRecordBizService;

    public TrainingRecordController(TrainingRecordBizService trainingRecordBizService) {
        this.trainingRecordBizService = trainingRecordBizService;
    }

    @Operation(summary = "训练打卡")
    @PostMapping("/checkin")
    public Result<?> checkin(@Valid @RequestBody TrainingCheckinDTO dto) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return Result.fail("未登录");
        }
        boolean ok = trainingRecordBizService.checkin(loginUser.getUserId(), dto);
        return ok ? Result.ok("打卡成功") : Result.fail("打卡失败，请先订阅计划");
    }

    @Operation(summary = "我的打卡列表")
    @GetMapping("/my/list")
    public Result<?> myList(@RequestParam(defaultValue = "20") Integer limit) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return Result.fail("未登录");
        }
        return Result.ok(trainingRecordBizService.listByUser(loginUser.getUserId(), limit));
    }

    @Operation(summary = "我的7日统计")
    @GetMapping("/my/weekly-stat")
    public Result<?> weeklyStat() {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            return Result.fail("未登录");
        }
        return Result.ok(trainingRecordBizService.weeklyStat(loginUser.getUserId()));
    }
}
