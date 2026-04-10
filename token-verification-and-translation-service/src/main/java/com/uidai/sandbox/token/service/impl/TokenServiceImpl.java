package com.uidai.sandbox.token.service.impl;

import com.uidai.sandbox.common.dto.TokenRequest;
import com.uidai.sandbox.common.dto.TokenResponse;
import com.uidai.sandbox.token.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of TokenService focusing on verification logic and normalization.
 * Uses a Redis-cached JWKS for efficient signature validation.
 */
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    private final JwtDecoder jwtDecoder;

    public TokenServiceImpl(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public TokenResponse verifyAndTranslate(TokenRequest request) {
        log.info("Processing token verification for System ID: {}", request.getSystemId());

        try {
            // 1. Validate JWT signature using JWKS (cached in Redis via SecurityConfig/JwksService)
            Jwt jwt = jwtDecoder.decode(request.getToken());
            
            log.info("Token verified successfully for subject: {}", jwt.getSubject());

            // 2. Check token expiration and claims (handled by decoder, but can be expanded)
            
            // 3. Translate foreign token structure to standardized UIDAI internal DTO
            return TokenResponse.builder()
                    .status("VERIFIED")
                    .message("Token successfully verified for " + request.getSystemId())
                    .details(Map.of(
                            "processedAt", Instant.now().toString(),
                            "systemId", request.getSystemId(),
                            "subject", jwt.getSubject(),
                            "trustLevel", "HIGH",
                            "expiresAt", Optional.ofNullable(jwt.getExpiresAt()).map(Instant::toString).orElse("N/A")
                    ))
                    .build();
                    
        } catch (Exception e) {
            log.error("Token verification failed for system {}: {}", request.getSystemId(), e.getMessage());
            return TokenResponse.builder()
                    .status("FAILED")
                    .message("Token verification failed: " + e.getMessage())
                    .details(Map.of(
                            "processedAt", Instant.now().toString(),
                            "systemId", request.getSystemId(),
                            "trustLevel", "NONE"
                    ))
                    .build();
        }
    }
}
