package com.uidai.sandbox.gateway.service.impl;

import com.uidai.sandbox.common.dto.TokenRequest;
import com.uidai.sandbox.common.dto.TokenResponse;
import com.uidai.sandbox.gateway.service.GatewayService;
import com.uidai.sandbox.gateway.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Implementation of GatewayService focusing on request routing and audit logging.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayServiceImpl implements GatewayService {

    private final KafkaProducerService kafkaProducerService;

    @Override
    public TokenResponse processIncomingRequest(TokenRequest request) {
        log.info("Gateway receiving request from system: {}", request.getSystemId());

        // Publish TokenRequest to Kafka for asynchronous processing
        kafkaProducerService.sendTokenRequest(request);
        
        return TokenResponse.builder()
                .status("ACCEPTED")
                .message("Request received at Gateway and queued for asynchronous processing.")
                .details(Map.of(
                        "gatewayId", "uidai-gateway-01",
                        "receivedAt", Instant.now().toString(),
                        "systemId", request.getSystemId(),
                        "deliveryMode", "ASYNC_KAFKA"
                ))
                .build();
    }
}
