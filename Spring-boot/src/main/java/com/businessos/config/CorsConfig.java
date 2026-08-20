package com.businessos.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // SSLCommerz's payment page redirects the payer's browser here with a cross-origin
        // POST (Origin: sandbox.sslcommerz.com) - the strict frontend-only rule below
        // rejected that as "Invalid CORS request" before the request ever reached the
        // controller, so the wallet/invoice was never credited even on a successful charge.
        // This endpoint is already permitAll() in SecurityConfig and sends no credentials,
        // so a permissive rule here is safe. Must be registered BEFORE "/**":
        // UrlBasedCorsConfigurationSource returns the config for the FIRST matching
        // pattern in registration order (not the most specific one), so registering
        // "/**" first would always win and this rule would never be reached.
        CorsConfiguration gatewayCallbackConfig = new CorsConfiguration();
        gatewayCallbackConfig.setAllowedOriginPatterns(List.of("*"));
        gatewayCallbackConfig.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        gatewayCallbackConfig.setAllowedHeaders(List.of("*"));
        gatewayCallbackConfig.setAllowCredentials(false);
        gatewayCallbackConfig.setMaxAge(3600L);
        source.registerCorsConfiguration("/api/payments/sslcommerz/callback/**", gatewayCallbackConfig);
        source.registerCorsConfiguration("/api/payments/sslcommerz/ipn", gatewayCallbackConfig);

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
