package com.uidai.sandbox.token.service;

import com.uidai.sandbox.common.config.KafkaTopicConfig;
import com.uidai.sandbox.common.dto.TokenRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Service for consuming Kafka messages and delegating to TokenService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final TokenService tokenService;

    /**
     * Listener for token verification requests.
     * @param request The token request received from Kafka.
     */
    @KafkaListener(topics = KafkaTopicConfig.TOKEN_VERIFICATION_TOPIC, groupId = "token-verification-group")
    public void consumeTokenRequest(TokenRequest request) {
        log.info("Received TokenRequest from Kafka topic: {} for system: {}", 
                KafkaTopicConfig.TOKEN_VERIFICATION_TOPIC, request.getSystemId());
        
        try {
            tokenService.verifyAndTranslate(request);
            log.info("Successfully processed TokenRequest for system: {}", request.getSystemId());
        } catch (Exception e) {
            log.error("Error processing TokenRequest for system: {}. Error: {}", 
                    request.getSystemId(), e.getMessage());
            // In a real scenario, we might want to send this to a Dead Letter Topic (DLT)
        }
    }
}
