package com.huang.web.admin.custom.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(
                new Info()
                        .title("健身平台管理端API")
                        .version("1.0")
                        .description("健身平台后台管理系统API - Spring Boot 3.0.5 + MyBatis-Plus 3.5.3.1"));
    }

    // ================== 用户权限管理模块 ==================
    @Bean
    public GroupedOpenApi userManagementAPI() {
        return GroupedOpenApi.builder()
                .group("01-用户权限管理")
                .pathsToMatch(
                        "/admin/user/**",
                        "/admin/role/**",
                        "/admin/user-role/**"
                )
                .build();
    }

    // ================== 教练管理模块 ==================
    @Bean
    public GroupedOpenApi coachManagementAPI() {
        return GroupedOpenApi.builder()
                .group("02-教练管理")
                .pathsToMatch(
                        "/admin/coach/**",
                        "/admin/coach-availability/**",
                        "/admin/coach-schedule-change/**",
                        "/admin/coach-certification-apply/**",
                        "/admin/coach-resignation-apply/**",
                        "/admin/coach-service/**",
                        "/admin/coach-consultation/**",
                        "/admin/coach-evaluation/**",
                        "/admin/coach-income/**",
                        "/admin/coach-settlement/**"
                )
                .build();
    }

    // ================== 健康档案管理模块 ==================
    @Bean
    public GroupedOpenApi healthManagementAPI() {
        return GroupedOpenApi.builder()
                .group("03-健康档案管理")
                .pathsToMatch(
                        "/admin/health-record/**",
                        "/admin/body-measurement/**"
                )
                .build();
    }

    // ================== 课程管理模块 ==================
    @Bean
    public GroupedOpenApi courseManagementAPI() {
        return GroupedOpenApi.builder()
                .group("04-课程管理")
                .pathsToMatch(
                        "/admin/course-category/**",
                        "/admin/course/**",
                        "/admin/course-schedule/**",
                        "/admin/course-enrollment/**"
                )
                .build();
    }

    // ================== 运动计划管理模块 ==================
    @Bean
    public GroupedOpenApi exerciseManagementAPI() {
        return GroupedOpenApi.builder()
                .group("05-运动计划管理")
                .pathsToMatch(
                        "/admin/exercise-record/**",
                        "/admin/exercise-plan/**"
                )
                .build();
    }

    // ================== 饮食营养管理模块 ==================
    @Bean
    public GroupedOpenApi nutritionManagementAPI() {
        return GroupedOpenApi.builder()
                .group("06-饮食营养管理")
                .pathsToMatch(
                        "/admin/food-database/**",
                        "/admin/diet-record/**"
                )
                .build();
    }

    // ================== 社交动态管理模块 ==================
    @Bean
    public GroupedOpenApi socialManagementAPI() {
        return GroupedOpenApi.builder()
                .group("07-社交动态管理")
                .pathsToMatch(
                        "/admin/post/**",
                        "/admin/comment/**",
                        "/admin/like-record/**",
                        "/admin/follow/**"
                )
                .build();
    }

    // ================== 健康科普管理模块 ==================
    @Bean
    public GroupedOpenApi articleManagementAPI() {
        return GroupedOpenApi.builder()
                .group("08-健康科普管理")
                .pathsToMatch(
                        "/admin/article-category/**",
                        "/admin/health-article/**",
                        "/admin/article-audit-log/**",
                        "/admin/article-view-log/**",
                        "/admin/article-like/**",
                        "/admin/article-collect/**",
                        "/admin/article-collect-folder/**",
                        "/admin/article-share-log/**",
                        "/admin/article-comment/**"
                )
                .build();
    }

    // ================== 订单支付管理模块 ==================
    @Bean
    public GroupedOpenApi orderPaymentManagementAPI() {
        return GroupedOpenApi.builder()
                .group("09-订单支付管理")
                .pathsToMatch(
                        "/admin/order-info/**",
                        "/admin/order-detail/**",
                        "/admin/payment-record/**",
                        "/admin/refund-record/**"
                )
                .build();
    }

    // ================== 系统管理模块 ==================
    @Bean
    public GroupedOpenApi systemManagementAPI() {
        return GroupedOpenApi.builder()
                .group("10-系统管理")
                .pathsToMatch(
                        "/admin/login/**",
                        "/admin/info",
                        "/admin/system-config/**",
                        "/admin/operation-log/**",
                        "/admin/dashboard/**",
                        "/admin/statistics/**"
                )
                .build();
    }
}