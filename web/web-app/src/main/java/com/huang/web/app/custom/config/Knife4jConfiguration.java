package com.huang.web.app.custom.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
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
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                // 为所有接口添加JWT认证要求
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT认证Token，请在值前面加上'Bearer '前缀")
                                        .name("Authorization")
                                        .in(SecurityScheme.In.HEADER)));
    }

    // ================== 用户认证模块 ==================
    @Bean
    public GroupedOpenApi authAPI() {
        return GroupedOpenApi.builder()
                .group("01-用户认证")
                .pathsToMatch(
                        "/app/auth/**"
                )
                .build();
    }

    // ================== 个人信息管理模块 ==================
    @Bean
    public GroupedOpenApi profileAPI() {
        return GroupedOpenApi.builder()
                .group("02-个人信息管理")
                .pathsToMatch(
                        "/app/profile/**"
                )
                .build();
    }

    // ================== 角色管理模块 ==================
    @Bean
    public GroupedOpenApi roleAPI() {
        return GroupedOpenApi.builder()
                .group("03-角色管理")
                .pathsToMatch(
                        "/app/role/**"
                )
                .build();
    }

    // ================== 教练认证申请模块 ==================
    @Bean
    public GroupedOpenApi coachCertificationAPI() {
        return GroupedOpenApi.builder()
                .group("04-教练认证申请")
                .pathsToMatch(
                        "/app/coach/certification/**"
                )
                .build();
    }

    // ================== 教练离职管理模块 ==================
    @Bean
    public GroupedOpenApi coachResignationAPI() {
        return GroupedOpenApi.builder()
                .group("05-教练离职管理")
                .pathsToMatch(
                        "/app/coach/resignation/**"
                )
                .build();
    }

    // ================== 教练咨询模块 ==================
    @Bean
    public GroupedOpenApi consultationAPI() {
        return GroupedOpenApi.builder()
                .group("06-教练咨询")
                .pathsToMatch(
                        "/app/consultation/**"
                )
                .build();
    }

    // ================== 教练日程管理模块 ==================
    @Bean
    public GroupedOpenApi coachScheduleAPI() {
        return GroupedOpenApi.builder()
                .group("07-教练日程管理")
                .pathsToMatch(
                        "/app/coach/schedule/**"
                )
                .build();
    }

}
