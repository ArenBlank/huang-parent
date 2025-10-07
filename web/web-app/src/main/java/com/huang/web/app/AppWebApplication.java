package com.huang.web.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = {"com.huang.web.app", "com.huang.common", "com.huang.model"})
@MapperScan("com.huang.web.app.mapper")
public class AppWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppWebApplication.class, args);
    }
}