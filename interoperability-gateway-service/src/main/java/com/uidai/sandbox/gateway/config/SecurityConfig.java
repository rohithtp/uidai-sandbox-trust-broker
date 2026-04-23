package com.uidai.sandbox.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the Interoperability Gateway Service.
 *
 * <p>This is a sandbox/development environment — all API endpoints are open to allow
 * E2E testing and system integration without credential management overhead.
 * In a production deployment this would be replaced with proper authentication
 * (e.g., mutual TLS, API-key header validation, or an OAuth2 client-credentials flow).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
