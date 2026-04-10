package com.uidai.sandbox.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.uidai.sandbox")
public class InteroperabilityGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(InteroperabilityGatewayApplication.class, args);
    }
}
