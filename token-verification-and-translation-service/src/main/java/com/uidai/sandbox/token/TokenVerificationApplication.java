package com.uidai.sandbox.token;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.uidai.sandbox")
public class TokenVerificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(TokenVerificationApplication.class, args);
    }
}
