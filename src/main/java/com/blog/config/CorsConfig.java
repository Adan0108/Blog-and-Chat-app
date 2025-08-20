 package com.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // Allow frontend origin(s)
        cfg.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://127.0.0.1:3000"
        ));
        // If need wildcard during dev, use:
        // cfg.setAllowedOriginPatterns(List.of("*"));

        // ✅ Methods & headers  app uses
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of(
                "Authorization", "Content-Type",
                "x-client-id", "x-rtoken-id", "User-Agent"
        ));

        // Optional: what the browser JS can read back
        cfg.setExposedHeaders(List.of("Authorization", "x-client-id"));

        // Use cookies only if  actually send them (we're using Bearer token → can be false)
        cfg.setAllowCredentials(false);

        cfg.setMaxAge(3600L); // cache preflight for 1h

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
