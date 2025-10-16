package com.huang.web.app.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.common.result.Result;
import com.huang.web.app.dto.store.GymStoreQueryDTO;
import com.huang.web.app.service.AppGymStoreService;
import com.huang.web.app.vo.store.GymStoreDetailVO;
import com.huang.web.app.vo.store.GymStoreListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * App端门店查询Controller
 * @author system
 * @since 2025-01-24
 */
@Tag(name = "门店信息", description = "门店查询相关接口")
@RestController
@RequestMapping("/app/gym-store")
@RequiredArgsConstructor
@Slf4j
public class AppGymStoreController {

    private final AppGymStoreService appGymStoreService;

    @Operation(summary = "分页查询门店列表", description = "查询所有营业中的门店,支持按名称、地址筛选")
    @GetMapping("/page")
    public Result<Page<GymStoreListVO>> page(@ModelAttribute GymStoreQueryDTO queryDTO) {
        Page<GymStoreListVO> page = appGymStoreService.getStoreListPage(queryDTO);
        return Result.success(page);
    }

    @Operation(summary = "查询门店详情", description = "根据门店ID查询详细信息,如果提供位置会计算距离")
    @GetMapping("/{id}")
    public Result<GymStoreDetailVO> detail(
            @Parameter(description = "门店ID", required = true) @PathVariable Long id,
            @Parameter(description = "用户当前纬度") @RequestParam(required = false) Double userLatitude,
            @Parameter(description = "用户当前经度") @RequestParam(required = false) Double userLongitude) {
        GymStoreDetailVO detailVO = appGymStoreService.getStoreDetail(id, userLatitude, userLongitude);
        return Result.success(detailVO);
    }

    @Operation(summary = "查询附近门店", description = "基于用户位置查询附近的营业中门店,按距离排序")
    @GetMapping("/nearby")
    public Result<List<GymStoreListVO>> nearby(
            @Parameter(description = "用户当前纬度", required = true) @RequestParam Double userLatitude,
            @Parameter(description = "用户当前经度", required = true) @RequestParam Double userLongitude,
            @Parameter(description = "搜索半径(公里),默认5公里,最大50公里") @RequestParam(required = false, defaultValue = "5") Integer radius) {
        List<GymStoreListVO> stores = appGymStoreService.getNearbyStores(userLatitude, userLongitude, radius);
        return Result.success(stores);
    }

    @Operation(summary = "搜索门店", description = "根据关键词搜索门店(搜索名称和地址)")
    @GetMapping("/search")
    public Result<Page<GymStoreListVO>> search(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Long size) {
        Page<GymStoreListVO> page = appGymStoreService.searchStores(keyword, current, size);
        return Result.success(page);
    }

    @Operation(summary = "查询门店的教练列表", description = "获取指定门店的所有在职教练,按评分排序")
    @GetMapping("/{id}/coaches")
    public Result<List<Map<String, Object>>> coaches(
            @Parameter(description = "门店ID", required = true) @PathVariable Long id) {
        List<Map<String, Object>> coaches = appGymStoreService.getStoreCoaches(id);
        return Result.success(coaches);
    }

    @Operation(summary = "查询门店的课程排期", description = "获取指定门店今日及未来的课程排期")
    @GetMapping("/{id}/courses")
    public Result<List<Map<String, Object>>> courses(
            @Parameter(description = "门店ID", required = true) @PathVariable Long id) {
        List<Map<String, Object>> courses = appGymStoreService.getStoreCourses(id);
        return Result.success(courses);
    }
}
