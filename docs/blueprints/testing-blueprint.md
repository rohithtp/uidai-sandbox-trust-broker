# Testing Blueprint: UIDAI Sandbox Trust Broker

## Overview
This document outlines the testing strategy for the Trust Broker project, ensuring reliability, security compliance, and performance across all interoperability layers.

## Testing Layers

### 1. Unit Testing
- **Scope**: Individual business logic in controllers, services, and utility classes.
- **Frameworks**: JUnit 5, Mockito.
- **Goals**: 
    - 100% coverage of translation logic.
    - Validate DTO validation constraints.
    - Mock all external dependencies (Kafka template, Redis template, other Feign/Rest clients).

### 2. Integration Testing
- **Scope**: Component interactions and infrastructure integration.
- **Frameworks**: Spring Boot Test, Testcontainers.
- **Key Scenarios**:
    - **Persistence/Cache**: Verify Redis caching logic for token sessions.
    - **Messaging**: Verify Kafka producer/consumer behavior using `@EmbeddedKafka` or Testcontainers.
    - **Service-to-Service**: Use `MockRestServiceServer` or WireMock to simulate calls between Gateway and Token service.

### 3. API & Contract Testing
- **Scope**: Ensuring stable interfaces between services.
- **Tools**: Spring Cloud Contract or Pact.
- **Goals**:
    - Define contracts for the `Token Verification` API.
    - Ensure `Gateway` can handle all response variants (Success, Expired, Malformed).

### 4. Security Testing
- **Scope**: Token verification and protocol safety.
- **Focus**:
    - Replay attack prevention.
    - Signature validation failure handling.
    - Expired token rejection.

### 5. Performance & Load Testing
- **Scope**: High-throughput token translation.
- **Tools**: JMeter or Gatling.
- **Metrics**: 
    - Latency (p95 < 200ms).
    - Throughput (Targeting 500 TPS).

## Automation & CI/CD
- **Pre-commit**: Run `./mvnw test` for changed modules.
- **CI Pipeline**:
    - Run full test suite on every PR.
    - Perform static analysis (SonarQube/Checkstyle).
    - Deploy to `sandbox-test` environment if all tests pass.

## Test Data Management
- Use `src/test/resources/payloads/` for standard JWT and DTO samples.
- Mock IDP responses for different scenarios (UIDAI, MockIDP).
