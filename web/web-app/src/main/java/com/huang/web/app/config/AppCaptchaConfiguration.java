package com.huang.web.app.config;

import com.anji.captcha.config.AjCaptchaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * AJ-Captcha 1.4.x still publishes Spring Boot 2 style auto configuration.
 * Import it explicitly so the app can run on Spring Boot 3.
 */
@Configuration
@Import(AjCaptchaAutoConfiguration.class)
public class AppCaptchaConfiguration {
}
