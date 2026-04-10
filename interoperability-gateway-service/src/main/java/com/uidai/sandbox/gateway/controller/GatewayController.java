package com.uidai.sandbox.gateway.controller;

import com.uidai.sandbox.common.dto.TokenRequest;
import com.uidai.sandbox.common.dto.TokenResponse;
import com.uidai.sandbox.gateway.service.GatewayService;
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
 * Controller for the Interoperability Gateway, handling entry-point routing and health.
 */
@RestController
@RequestMapping("/api/v1/gateway")
@RequiredArgsConstructor
@Tag(name = "Gateway", description = "Endpoints for inter-system routing and gateway health.")
public class GatewayController {

    private final GatewayService gatewayService;

    @Operation(summary = "Process Incoming Request", description = "Accepts requests from external systems and routes them for verification and translation.")
    @PostMapping("/process")
    public ResponseEntity<TokenResponse> processRequest(@RequestBody TokenRequest request) {
        return ResponseEntity.ok(gatewayService.processIncomingRequest(request));
    }

    @Operation(summary = "Health Check", description = "Returns the status of the Interoperability Gateway service.")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "interoperability-gateway-service",
                "status", "UP"
        ));
    }
}
