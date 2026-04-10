package com.uidai.sandbox.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI trustBrokerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UIDAI Sandbox Trust Broker API")
                        .description("Centralized security layer for verifying and translating tokens between identity providers and UIDAI sandbox systems.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Trust Broker Team")
                                .email("support@uidai-sandbox.gov.in"))
                        .license(new License()
                                .name("Internal Use Only")
                                .url("https://uidai-sandbox.gov.in/license")));
    }
}
