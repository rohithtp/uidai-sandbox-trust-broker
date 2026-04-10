package com.uidai.sandbox.gateway.service;

import com.uidai.sandbox.common.dto.TokenRequest;

/**
 * Service for producing Kafka messages to various topics.
 */
public interface KafkaProducerService {
    
    /**
     * Sends a TokenRequest event to the verification topic.
     * @param request The token request to send.
     */
    void sendTokenRequest(TokenRequest request);

    /**
     * Sends a TokenRequest event to a specific topic.
     * @param topic The target Kafka topic.
     * @param request The token request to send.
     */
    void sendToTopic(String topic, TokenRequest request);
}
