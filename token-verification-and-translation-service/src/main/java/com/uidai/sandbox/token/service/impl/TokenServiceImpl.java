package com.uidai.sandbox.token.service.impl;

import com.uidai.sandbox.common.dto.TokenRequest;
import com.uidai.sandbox.common.dto.TokenResponse;
import com.uidai.sandbox.token.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
    private final JwtEncoder jwtEncoder;

    public TokenServiceImpl(JwtDecoder jwtDecoder, JwtEncoder jwtEncoder) {
        this.jwtDecoder = jwtDecoder;
        this.jwtEncoder = jwtEncoder;
    }

    @Override
    public TokenResponse verifyAndTranslate(TokenRequest request) {
        log.info("Processing token verification for System ID: {}", request.getSystemId());

        try {
            // 1. Validate JWT signature using JWKS (cached in Redis via SecurityConfig/JwksService)
            Jwt jwt = jwtDecoder.decode(request.getToken());
            
            log.info("Token verified successfully for subject: {}", jwt.getSubject());

            // 2. Check token expiration and claims (handled by decoder, but can be expanded)
            
            // 3. Translate/Issue Sandbox Session Token
            Instant now = Instant.now();
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer("uidai-trust-broker")
                    .issuedAt(now)
                    .expiresAt(now.plus(Duration.ofMinutes(30)))
                    .subject(jwt.getSubject())
                    .claim("originSystem", request.getSystemId())
                    .claim("trustLevel", "HIGH")
                    .build();

            String sessionToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

            // 4. Build standardized UIDAI internal DTO
            return TokenResponse.builder()
                    .status("VERIFIED")
                    .message("Token successfully verified and translated for " + request.getSystemId())
                    .translatedToken(sessionToken)
                    .details(Map.of(
                            "processedAt", now.toString(),
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
