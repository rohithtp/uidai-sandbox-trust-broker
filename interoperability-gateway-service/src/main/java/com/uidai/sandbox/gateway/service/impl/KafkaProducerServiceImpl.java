package com.uidai.sandbox.gateway.service.impl;

import com.uidai.sandbox.common.config.KafkaTopicConfig;
import com.uidai.sandbox.common.dto.TokenRequest;
import com.uidai.sandbox.gateway.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Implementation of KafkaProducerService using Spring KafkaTemplate.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerServiceImpl implements KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void sendTokenRequest(TokenRequest request) {
        sendToTopic(KafkaTopicConfig.TOKEN_VERIFICATION_TOPIC, request);
    }

    @Override
    public void sendToTopic(String topic, TokenRequest request) {
        log.info("Sending TokenRequest to Kafka topic: {} for system: {}", 
                topic, request.systemId());
        
        kafkaTemplate.send(topic, request.systemId(), request)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully sent message to topic {} with offset: {}", 
                                topic, result.getRecordMetadata().offset());
                    } else {
                        log.error("Unable to send message to topic {} due to: {}", topic, ex.getMessage());
                    }
                });
    }
}
