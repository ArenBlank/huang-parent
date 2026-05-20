package com.huang.web.app.custom.config;

import com.huang.web.app.custom.interceptor.AuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private AuthenticationInterceptor authenticationInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // App side APIs need JWT authentication, but auth and captcha endpoints are public.
        registry.addInterceptor(this.authenticationInterceptor)
                .addPathPatterns("/app/**")
                .excludePathPatterns("/app/auth/captcha/get",
                        "/app/auth/captcha/check",
                        "/app/auth/**",
                        "/app/test/**",
                        "/app/pay/callback",
                        "/app/banner/list",
                        "/app/notice/list",
                        "/app/system-config/map",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**");
    }
}
