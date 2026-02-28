package com.huang.web.admin.custom.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfiguration {

    @Bean
    public OpenAPI adminOpenApi() {
        return new OpenAPI().info(
                new Info()
                        .title("Fitness Platform Admin API")
                        .version("1.0")
                        .description("Admin side API docs for booking/order/user/role operations.")
        );
    }

    @Bean
    public GroupedOpenApi adminAllGroup() {
        return GroupedOpenApi.builder()
                .group("admin-all")
                .pathsToMatch("/admin/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminUserRoleGroup() {
        return GroupedOpenApi.builder()
                .group("admin-user-role")
                .pathsToMatch(
                        "/admin/user/**",
                        "/admin/role/**",
                        "/admin/user-role/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi adminOpsGroup() {
        return GroupedOpenApi.builder()
                .group("admin-ops-dashboard-video")
                .pathsToMatch("/admin/ops/**", "/admin/dashboard/**", "/admin/video/**", "/admin/course/**", "/admin/refund/**", "/admin/pay/**")
                .build();
    }
}
