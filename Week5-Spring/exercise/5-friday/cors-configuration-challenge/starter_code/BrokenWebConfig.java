package com.example.books.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * BROKEN CORS CONFIGURATION — Debug Exercise (Phase 3)
 *
 * This WebConfig contains exactly 3 bugs. Find them, explain the symptom
 * each causes, and submit your fixed version as FixedWebConfig.java.
 *
 * Hints:
 *  - One bug causes a Spring Boot startup failure.
 *  - One bug causes CORS to silently fail for DELETE and PUT requests.
 *  - One bug is a security misconfiguration that would expose the API to any origin.
 */
@Configuration
public class BrokenWebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")                              // Bug? Investigate.
                .allowedMethods("GET", "POST")                    // Bug? Investigate.
                .allowedHeaders("Content-Type")
                .allowCredentials(true)                           // Bug? Investigate.
                .maxAge(3600);
    }
}
