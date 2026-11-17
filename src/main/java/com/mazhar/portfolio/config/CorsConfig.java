package com.mazhar.portfolio.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets the Angular app call this API from the browser. The allowed origins come from
 * {@code cors.allowed-origins} (comma-separated), defaulting to the Angular dev server, so a
 * deployment sets its real site origin in configuration instead of editing code.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${cors.allowed-origins:http://localhost:4200}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // With no origins configured, skip the mapping entirely: allowedOrigins() with an
        // empty array would reject all cross-origin calls anyway, but an empty list must never
        // be read as "allow everything".
        if (allowedOrigins.length == 0) {
            return;
        }
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
