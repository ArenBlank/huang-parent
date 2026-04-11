package com.huang.web.app.custom.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class Knife4jConfiguration {

    private static final String BEARER_AUTH = "BearerAuth";
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/app/pay/callback",
            "/app/banner/list",
            "/app/notice/list",
            "/app/system-config/map"
    );

    @Bean
    public OpenAPI appOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(
                        BEARER_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("Use `Bearer <accessToken>` for app protected APIs.")
                ))
                .info(new Info()
                        .title("Fitness Platform App API")
                        .version("1.0")
                        .description("App side API docs for auth, profile, plan, record, booking, course and callback flows."));
    }

    @Bean
    public OpenApiCustomizer appSecurityCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, pathItem) -> {
                if (path == null || path.startsWith("/app/auth/") || PUBLIC_PATHS.contains(path)) {
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
    public GroupedOpenApi appAllGroup(OpenApiCustomizer appSecurityCustomizer) {
        return GroupedOpenApi.builder()
                .group("app-all")
                .pathsToMatch("/app/**")
                .addOpenApiCustomizer(appSecurityCustomizer)
                .build();
    }

    @Bean
    public GroupedOpenApi appAuthProfileGroup(OpenApiCustomizer appSecurityCustomizer) {
        return GroupedOpenApi.builder()
                .group("app-auth-profile")
                .pathsToMatch(
                        "/app/auth/**",
                        "/app/profile/**"
                )
                .addOpenApiCustomizer(appSecurityCustomizer)
                .build();
    }

    @Bean
    public GroupedOpenApi appTrainingGroup(OpenApiCustomizer appSecurityCustomizer) {
        return GroupedOpenApi.builder()
                .group("app-plan-record-booking")
                .pathsToMatch("/app/plan/**", "/app/record/**", "/app/booking/**", "/app/course/**")
                .addOpenApiCustomizer(appSecurityCustomizer)
                .build();
    }

    @Bean
    public GroupedOpenApi appPaymentCallbackGroup(OpenApiCustomizer appSecurityCustomizer) {
        return GroupedOpenApi.builder()
                .group("app-payment-callback")
                .pathsToMatch("/app/pay/**")
                .addOpenApiCustomizer(appSecurityCustomizer)
                .build();
    }
}
