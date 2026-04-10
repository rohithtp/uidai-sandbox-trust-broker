package com.uidai.sandbox.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight health/status endpoint for the Interoperability Gateway.
 */
@RestController
@RequestMapping("/api/v1/gateway")
@Tag(name = "Gateway", description = "Endpoints for inter-system routing and gateway health.")
public class GatewayController {

    @Operation(summary = "Health Check", description = "Returns the status of the Interoperability Gateway service.")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "interoperability-gateway-service",
                "status", "UP"
        ));
    }
}
