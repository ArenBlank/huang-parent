package com.huang.web.app.custom.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfiguration {

    @Bean
    public OpenAPI appOpenApi() {
        return new OpenAPI().info(
                new Info()
                        .title("Fitness Platform App API")
                        .version("1.0")
                        .description("App side API docs for auth/profile/plan/record/booking flows.")
        );
    }

    @Bean
    public GroupedOpenApi appAllGroup() {
        return GroupedOpenApi.builder()
                .group("app-all")
                .pathsToMatch("/app/**")
                .build();
    }

    @Bean
    public GroupedOpenApi appAuthProfileGroup() {
        return GroupedOpenApi.builder()
                .group("app-auth-profile")
                .pathsToMatch(
                        "/app/auth/**",
                        "/app/profile/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi appTrainingGroup() {
        return GroupedOpenApi.builder()
                .group("app-plan-record-booking")
                .pathsToMatch("/app/plan/**", "/app/record/**", "/app/booking/**")
                .build();
    }
}
