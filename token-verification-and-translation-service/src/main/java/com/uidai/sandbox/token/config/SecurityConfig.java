package com.uidai.sandbox.token.config;

import com.uidai.sandbox.token.service.JwksService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwksService jwksService;

    public SecurityConfig(JwksService jwksService) {
        this.jwksService = jwksService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/token/**").permitAll() // Allowing API calls to be handled by controller logic
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            );
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Custom JwtDecoder that uses JwksService (which is cached in Redis)
        com.nimbusds.jose.jwk.source.JWKSource<com.nimbusds.jose.proc.SecurityContext> jwkSource = (jwkSelector, context) -> {
            try {
                return jwkSelector.select(jwksService.getJWKSet());
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve JWKS for token verification", e);
            }
        };

        com.nimbusds.jwt.proc.ConfigurableJWTProcessor<com.nimbusds.jose.proc.SecurityContext> jwtProcessor = 
            new com.nimbusds.jwt.proc.DefaultJWTProcessor<>();
        
        // We support RS256 as a common default for UIDAI/OIDC tokens
        jwtProcessor.setJWSKeySelector(new com.nimbusds.jose.proc.JWSVerificationKeySelector<>(
            com.nimbusds.jose.JWSAlgorithm.RS256, jwkSource));
            
        return new NimbusJwtDecoder(jwtProcessor);
    }
}
