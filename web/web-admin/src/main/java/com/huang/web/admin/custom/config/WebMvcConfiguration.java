package com.huang.web.admin.custom.config;

import com.huang.web.admin.custom.converter.StringToBaseEnumConverterFactory;
import com.huang.web.admin.custom.interceptor.AuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private AuthenticationInterceptor authenticationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册认证拦截器，应用于所有admin路径
        registry.addInterceptor(this.authenticationInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns(
                    "/admin/doc.html",       // Knife4j文档页面
                    "/admin/v3/**",          // OpenAPI文档
                    "/admin/swagger-ui/**",  // Swagger UI资源
                    "/admin/webjars/**",     // 静态资源
                    "/admin/favicon.ico"     // 图标
                );
    }
}
