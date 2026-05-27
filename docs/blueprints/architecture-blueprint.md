# Architecture Blueprint: UIDAI Sandbox Trust Broker

## Overview
The Trust Broker is a multi-module Spring Boot application designed to facilitate interoperability between different identity systems within the UIDAI sandbox environment. It handles protocol adaptation, token verification, and translation.

## Component Architecture

### 1. Interoperability Gateway Service (`8081`)
- **Role**: Entry point for all external requests.
- **Responsibilities**:
    - Route adaptation.
    - Protocol translation.
    - Request validation.
- **Tech Stack**: Spring Boot, Spring Web.

### 2. Token Verification & Translation Service (`8082`)
- **Role**: Security and data normalization hub.
- **Responsibilities**:
    - JWT/OAuth2 token verification.
    - Format translation (e.g., between different IDP formats).
    - Cross-system token mapping.
- **Tech Stack**: Spring Security, JJWT.

### 3. Trust Broker Common
- **Role**: Shared library.
- **Responsibilities**:
    - Shared DTOs (`TokenRequest`, `TokenResponse`, `AuditEvent`).
    - Exception handling (`GlobalExceptionHandler`).
    - Kafka/Redis configurations.

## Infrastructure & Integration
- **Messaging**: Kafka (for audit events and asynchronous processing).
- **Caching**: Redis (for token state and lookups).
- **API Documentation**: OpenAPI/Swagger.

## Component Flow
1. **Source System** POSTs token request to the Gateway.
2. Gateway checks **System Registry**, extracts `TrustLevel`, and evaluates **RoutingRules**.
3. If allowed, Gateway publishes `TokenRequest` to Kafka `token-verification` topic.
4. **Token Verification Service** consumes the Kafka message.
5. Service fetches target JWKS (cached in Redis), verifies token signature, checks trust level.
6. Service issues a new internal Sandbox Session Token and writes to Audit Log.

## Platform & Runtime
- **JDK**: 25 LTS (Virtual Threads, Records, Pattern Matching, Sealed Interfaces, Scoped Values)
- **Spring Boot**: 3.5.14 (virtual thread auto-configuration, improved observability)
- **Concurrency Model**: Virtual threads (`spring.threads.virtual.enabled=true`) for all I/O-bound request handling

## Data Flow
1. Client sends request to **Gateway**.
2. Gateway extracts tokens and calls **Token Service**.
3. Token Service verifies authenticity and translates payload.
4. Gateway routes the request to the target system with the translated token.
5. All critical events are published to **Kafka** for auditing.
