package com.huang.web.app.custom.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("健身平台用户端API")
                        .version("1.0")
                        .description("健身平台用户端APP接口 - Spring Boot 3.0.5 + MyBatis-Plus 3.5.3.1")
                        .termsOfService("https://github.com/your-project")
                        .license(new License().name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }

    // ================== 用户认证模块 ==================
    @Bean
    public GroupedOpenApi authAPI() {
        return GroupedOpenApi.builder()
                .group("01-用户认证")
                .pathsToMatch(
                        "/app/login/**",
                        "/app/register/**",
                        "/app/logout/**",
                        "/app/info",
                        "/app/profile/**"
                )
                .build();
    }

    // ================== 健康档案模块 ==================
    @Bean
    public GroupedOpenApi healthAPI() {
        return GroupedOpenApi.builder()
                .group("02-健康档案")
                .pathsToMatch(
                        "/app/health-record/**",
                        "/app/body-measurement/**"
                )
                .build();
    }

    // ================== 运动健身模块 ==================
    @Bean
    public GroupedOpenApi exerciseAPI() {
        return GroupedOpenApi.builder()
                .group("03-运动健身")
                .pathsToMatch(
                        "/app/exercise-record/**",
                        "/app/exercise-plan/**"
                )
                .build();
    }

    // ================== 饮食营养模块 ==================
    @Bean
    public GroupedOpenApi nutritionAPI() {
        return GroupedOpenApi.builder()
                .group("04-饮食营养")
                .pathsToMatch(
                        "/app/food-database/**",
                        "/app/diet-record/**"
                )
                .build();
    }

    // ================== 课程学习模块 ==================
    @Bean
    public GroupedOpenApi courseAPI() {
        return GroupedOpenApi.builder()
                .group("05-课程学习")
                .pathsToMatch(
                        "/app/course-category/**",
                        "/app/course/**",
                        "/app/course-schedule/**",
                        "/app/course-enrollment/**",
                        "/app/my-courses/**"
                )
                .build();
    }

    // ================== 教练服务模块 ==================
    @Bean
    public GroupedOpenApi coachServiceAPI() {
        return GroupedOpenApi.builder()
                .group("06-教练服务")
                .pathsToMatch(
                        "/app/coach/**",
                        "/app/coach-consultation/**",
                        "/app/coach-evaluation/**",
                        "/app/coach-booking/**"
                )
                .build();
    }

    // ================== 社交互动模块 ==================
    @Bean
    public GroupedOpenApi socialAPI() {
        return GroupedOpenApi.builder()
                .group("07-社交互动")
                .pathsToMatch(
                        "/app/post/**",
                        "/app/comment/**",
                        "/app/like/**",
                        "/app/follow/**",
                        "/app/moments/**"
                )
                .build();
    }

    // ================== 健康科普模块 ==================
    @Bean
    public GroupedOpenApi articleAPI() {
        return GroupedOpenApi.builder()
                .group("08-健康科普")
                .pathsToMatch(
                        "/app/article-category/**",
                        "/app/health-article/**",
                        "/app/article-like/**",
                        "/app/article-collect/**",
                        "/app/article-collect-folder/**",
                        "/app/article-share/**",
                        "/app/article-comment/**"
                )
                .build();
    }

    // ================== 订单支付模块 ==================
    @Bean
    public GroupedOpenApi orderAPI() {
        return GroupedOpenApi.builder()
                .group("09-订单支付")
                .pathsToMatch(
                        "/app/order/**",
                        "/app/payment/**",
                        "/app/refund/**",
                        "/app/my-orders/**"
                )
                .build();
    }

    // ================== 个人中心模块 ==================
    @Bean
    public GroupedOpenApi personalCenterAPI() {
        return GroupedOpenApi.builder()
                .group("10-个人中心")
                .pathsToMatch(
                        "/app/user/**",
                        "/app/settings/**",
                        "/app/feedback/**",
                        "/app/help/**",
                        "/app/about/**"
                )
                .build();
    }

}