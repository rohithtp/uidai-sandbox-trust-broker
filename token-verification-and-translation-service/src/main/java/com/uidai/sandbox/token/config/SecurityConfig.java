package com.uidai.sandbox.token.config;

import com.uidai.sandbox.token.service.JwksService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwksService jwksService;

    public SecurityConfig(JwksService jwksService) {
        // JDK 25: validation before field assignment (flexible constructor)
        if (jwksService == null) {
            throw new IllegalArgumentException("JwksService must not be null");
        }
        super();
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

    @Bean
    public JwtEncoder jwtEncoder() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        
        com.nimbusds.jose.jwk.RSAKey rsaKey = new com.nimbusds.jose.jwk.RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        
        com.nimbusds.jose.jwk.JWKSet jwkSet = new com.nimbusds.jose.jwk.JWKSet(rsaKey);
        com.nimbusds.jose.jwk.source.JWKSource<com.nimbusds.jose.proc.SecurityContext> jwks = 
                new com.nimbusds.jose.jwk.source.ImmutableJWKSet<>(jwkSet);
        
        return new NimbusJwtEncoder(jwks);
    }

    private static KeyPair generateRsaKey() {
        // TODO (JDK 25 KDF API): Future integration for HKDF-based symmetric key derivation 
        // when issuing specific sandbox session tokens, using javax.crypto.KDF.
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }
}
