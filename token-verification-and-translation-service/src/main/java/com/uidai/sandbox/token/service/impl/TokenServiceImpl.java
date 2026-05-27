package com.uidai.sandbox.token.service.impl;

import com.uidai.sandbox.common.dto.TokenRequest;
import com.uidai.sandbox.common.dto.TokenResponse;
import com.uidai.sandbox.common.dto.VerificationResult;
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
    public VerificationResult verifyAndTranslate(TokenRequest request) {
        log.info("Processing token verification for System ID: {}", request.systemId());

        try {
            // 1. Validate JWT signature using JWKS (cached in Redis via SecurityConfig/JwksService)
            Jwt jwt = jwtDecoder.decode(request.token());
            
            log.info("Token verified successfully for subject: {}", jwt.getSubject());

            // 2. Check token expiration and claims (handled by decoder, but can be expanded)
            
            // 3. Translate/Issue Sandbox Session Token
            // Here we explicitly add "additional fields" to the NEWLY SIGNED token.
            String originalName = jwt.getClaimAsString("name");
            String normalizedName = originalName != null ? originalName.toUpperCase() : "UNKNOWN";
            
            Instant now = Instant.now();
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer("uidai-trust-broker")
                    .issuedAt(now)
                    .expiresAt(now.plus(Duration.ofMinutes(30)))
                    .subject(jwt.getSubject())
                    .claim("originSystem", request.systemId())
                    .claim("trustLevel", "HIGH")
                    .claim("tokenType", "SANDBOX_SESSION_TOKEN")
                    .claim("normalizedName", normalizedName) // Proof: This is added to the signed token
                    .build();

            String sessionToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

            log.info("Issued new Sandbox Session Token for {} with normalized name: {}", jwt.getSubject(), normalizedName);

            // 4. Build standardized UIDAI internal DTO
            return new VerificationResult.Success(
                    jwt.getSubject(),
                    sessionToken,
                    Map.of(
                            "processedAt", now.toString(),
                            "systemId", request.systemId(),
                            "trustLevel", "HIGH",
                            "normalizedName", normalizedName,
                            "tokenIssued", "true",
                            "expiresAt", Optional.ofNullable(jwt.getExpiresAt()).map(Instant::toString).orElse("N/A")
                    )
            );
                    
        } catch (Exception e) {
            log.error("Token verification failed for system {}: {}", request.systemId(), e.getMessage());
            return new VerificationResult.Failure(e.getMessage(), request.systemId());
        }
    }
}
