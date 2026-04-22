package com.huang.web.admin.custom.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfiguration {

    private static final String BEARER_AUTH = "BearerAuth";

    @Bean
    public OpenAPI adminOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(
                        BEARER_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("Use `Bearer <accessToken>` for admin protected APIs.")
                ))
                .info(new Info()
                        .title("Fitness Platform Admin API")
                        .version("1.0")
                        .description("Admin side API docs for booking, order, user, role, payment audit and task operations."));
    }

    @Bean
    public OpenApiCustomizer adminSecurityCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, pathItem) -> {
                if (path == null || path.startsWith("/admin/auth/")) {
                    return;
                }
                pathItem.readOperations().forEach(operation -> {
                    if (operation.getSecurity() == null
                            || operation.getSecurity().stream().noneMatch(item -> item.containsKey(BEARER_AUTH))) {
                        operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
                    }
                });
            });
        };
    }

    @Bean
    public GroupedOpenApi adminAllGroup(OpenApiCustomizer adminSecurityCustomizer) {
        return GroupedOpenApi.builder()
                .group("admin-all")
                .pathsToMatch("/admin/**")
                .addOpenApiCustomizer(adminSecurityCustomizer)
                .build();
    }

    @Bean
    public GroupedOpenApi adminUserRoleGroup(OpenApiCustomizer adminSecurityCustomizer) {
        return GroupedOpenApi.builder()
                .group("admin-user-role")
                .pathsToMatch(
                        "/admin/user/**",
                        "/admin/role/**",
                        "/admin/user-role/**"
                )
                .addOpenApiCustomizer(adminSecurityCustomizer)
                .build();
    }

    @Bean
    public GroupedOpenApi adminOpsGroup(OpenApiCustomizer adminSecurityCustomizer) {
        return GroupedOpenApi.builder()
                .group("admin-ops-dashboard-video")
                .pathsToMatch(
                        "/admin/ops/**",
                        "/admin/dashboard/**",
                        "/admin/video/**",
                        "/admin/course/**",
                        "/admin/upload/**",
                        "/admin/refund/**",
                        "/admin/pay/**"
                )
                .addOpenApiCustomizer(adminSecurityCustomizer)
                .build();
    }

    @Bean
    public GroupedOpenApi adminAuthTaskGroup(OpenApiCustomizer adminSecurityCustomizer) {
        return GroupedOpenApi.builder()
                .group("admin-auth-task-run")
                .pathsToMatch("/admin/auth/**", "/admin/task-run/**")
                .addOpenApiCustomizer(adminSecurityCustomizer)
                .build();
    }

    @Bean
    public GroupedOpenApi adminPaymentAuditGroup(OpenApiCustomizer adminSecurityCustomizer) {
        return GroupedOpenApi.builder()
                .group("admin-payment-audit")
                .pathsToMatch("/admin/refund/**", "/admin/pay/**")
                .addOpenApiCustomizer(adminSecurityCustomizer)
                .build();
    }
}
