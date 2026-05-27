package com.uidai.sandbox.token.controller;

import com.uidai.sandbox.common.dto.TokenRequest;
import com.uidai.sandbox.common.dto.TokenResponse;
import com.uidai.sandbox.common.dto.VerificationResult;
import com.uidai.sandbox.token.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for token verification and translation operations.
 */
@RestController
@RequestMapping("/api/v1/token")
@RequiredArgsConstructor
@Tag(name = "Token Management", description = "Endpoints for token verification, translation, and security processing.")
public class TokenController {

    private final TokenService tokenService;

    /**
     * Accepts a raw token payload and returns a translated/verified response.
     */
    @Operation(summary = "Verify and Translate Token", description = "Validates the incoming token and translates it into a format compatible with UIDAI internal services.")
    @PostMapping("/verify")
    public ResponseEntity<TokenResponse> verifyToken(@RequestBody TokenRequest request) {
        return switch (tokenService.verifyAndTranslate(request)) {
            case VerificationResult.Success(var subject, var token, var details) -> 
                ResponseEntity.ok(new TokenResponse("VERIFIED", "Token successfully verified and translated", token, details));
            case VerificationResult.Failure(var reason, var systemId) -> 
                ResponseEntity.status(401).body(new TokenResponse("FAILED", "Token verification failed: " + reason, null, Map.of("systemId", systemId)));
        };
    }

    @Operation(summary = "Health Check", description = "Returns the status of the Token Verification and Translation service.")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "token-verification-and-translation-service",
                "status", "UP"
        ));
    }
}
