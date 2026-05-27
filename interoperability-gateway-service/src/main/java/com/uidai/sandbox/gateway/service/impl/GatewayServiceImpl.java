package com.uidai.sandbox.gateway.service.impl;

import com.uidai.sandbox.common.config.KafkaTopicConfig;
import com.uidai.sandbox.common.dto.TokenRequest;
import com.uidai.sandbox.common.dto.TokenResponse;
import com.uidai.sandbox.common.dto.TrustLevel;
import com.uidai.sandbox.gateway.service.GatewayService;
import com.uidai.sandbox.gateway.service.KafkaProducerService;
import com.uidai.sandbox.gateway.service.SystemRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;

/**
 * Implementation of GatewayService focusing on request routing and audit logging.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayServiceImpl implements GatewayService {

    private final KafkaProducerService kafkaProducerService;
    private final SystemRegistryService systemRegistryService;

    @Override
    public TokenResponse processIncomingRequest(TokenRequest request) {
        log.info("Gateway receiving request from system: {}", request.systemId());

        // 1. System and Trust Level Validation
        var systemOpt = systemRegistryService.getSystem(request.systemId());
        if (systemOpt.isEmpty()) {
            log.warn("System {} not found in registry. Rejecting request.", request.systemId());
            return buildRejectedResponse("System not registered", request.systemId());
        }

        var system = systemOpt.get();
        if (!system.isActive()) {
            log.warn("System {} is inactive. Rejecting request.", request.systemId());
            return buildRejectedResponse("System is inactive", request.systemId());
        }

        switch (system.getTrustLevel()) {
            case null -> {
                log.warn("System {} has insufficient trust level: null. Rejecting request.", request.systemId());
                return buildRejectedResponse("Insufficient trust level", request.systemId());
            }
            case LOW -> {
                log.warn("System {} has insufficient trust level: {}. Rejecting request.", 
                    request.systemId(), system.getTrustLevel());
                return buildRejectedResponse("Insufficient trust level", request.systemId());
            }
            case MEDIUM, HIGH, CRITICAL -> {
                // 2. Routing Logic based on RoutingRules
                var routingRules = systemRegistryService.getRoutingRules(request.systemId());
                if (routingRules.isEmpty()) {
                    log.warn("No routing rules found for system {}. Defaulting to KAFKA dispatch.", request.systemId());
                    // Fallback to default behavior if no rules defined
                    kafkaProducerService.sendTokenRequest(request);
                } else {
                    // Pick highest priority rule using Sequenced Collections (JDK 21+)
                    routingRules.sort(Comparator.comparingInt(com.uidai.sandbox.common.dto.RoutingRule::getPriority).reversed());
                    var selectedRule = routingRules.getFirst();

                    log.info("Routing request for system {} using rule: {} (Protocol: {}, Target: {})", 
                        request.systemId(), selectedRule.getRuleId(), selectedRule.getProtocol(), selectedRule.getTargetEndpoint());

                    if ("KAFKA".equalsIgnoreCase(selectedRule.getProtocol())) {
                        String topic = selectedRule.getTargetEndpoint() != null ? 
                                selectedRule.getTargetEndpoint() : KafkaTopicConfig.TOKEN_VERIFICATION_TOPIC;
                        kafkaProducerService.sendToTopic(topic, request);
                    } else {
                        log.warn("Rule specified protocol {}, but currently only KAFKA protocol is implemented for dispatch. Falling back to default topic.", 
                            selectedRule.getProtocol());
                        kafkaProducerService.sendTokenRequest(request);
                    }
                }
                
                return new TokenResponse(
                        "ACCEPTED",
                        "Request validated and routed for processing.",
                        null,
                        Map.of(
                                "gatewayId", "uidai-gateway-01",
                                "receivedAt", Instant.now().toString(),
                                "systemId", request.systemId(),
                                "trustLevel", system.getTrustLevel().toString(),
                                "deliveryMode", "ASYNC_ROUTED"
                        )
                );
            }
        }
    }

    private TokenResponse buildRejectedResponse(String reason, String systemId) {
        return new TokenResponse(
                "REJECTED",
                reason,
                null,
                Map.of(
                        "gatewayId", "uidai-gateway-01",
                        "rejectedAt", Instant.now().toString(),
                        "systemId", systemId
                )
        );
    }
}
