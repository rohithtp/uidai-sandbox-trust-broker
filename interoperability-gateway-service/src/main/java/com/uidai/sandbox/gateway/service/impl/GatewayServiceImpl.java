package com.uidai.sandbox.gateway.service.impl;

import com.uidai.sandbox.common.dto.TokenRequest;
import com.uidai.sandbox.common.dto.TokenResponse;
import com.uidai.sandbox.gateway.service.GatewayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Implementation of GatewayService focusing on request routing and audit logging.
 */
@Slf4j
@Service
public class GatewayServiceImpl implements GatewayService {

    @Override
    public TokenResponse processIncomingRequest(TokenRequest request) {
        log.info("Gateway receiving request from system: {}", request.getSystemId());

        // In a real implementation:
        // 1. Identify protocol transformation needed
        // 2. Publish AuditEvent to Kafka
        // 3. Delegate to TokenVerificationService (via Feign or Kafka)
        
        return TokenResponse.builder()
                .status("ACCEPTED")
                .message("Request received at Gateway and queued for processing.")
                .details(Map.of(
                        "gatewayId", "uidai-gateway-01",
                        "receivedAt", Instant.now().toString(),
                        "systemId", request.getSystemId()
                ))
                .build();
    }
}
