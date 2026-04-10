package com.uidai.sandbox.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String AUDIT_LOG_TOPIC = "trust-broker.audit.log";
    public static final String TOKEN_VERIFICATION_TOPIC = "trust-broker.token.verification";

    @Bean
    public NewTopic auditLogTopic() {
        return TopicBuilder.name(AUDIT_LOG_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic tokenVerificationTopic() {
        return TopicBuilder.name(TOKEN_VERIFICATION_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
