package com.mini3.backend.domain.ats.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * management-front 등에서 document-analyzer 로 직접 호출할 때 CORS.
 * 기본값은 {@code application.yml} 의 {@code app.cors.allowed-origins} 와 동일(팀 프론트 ELB).
 * 여러 출처는 쉼표로 구분. 운영에서는 {@code APP_CORS_ALLOWED_ORIGINS} 로 덮어쓴다.
 */
@Configuration
public class AtsCorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public WebMvcConfigurer atsCorsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] origins = Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toArray(String[]::new);
                registry.addMapping("/**")
                        .allowedOrigins(origins)
                        .allowedMethods("*")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
