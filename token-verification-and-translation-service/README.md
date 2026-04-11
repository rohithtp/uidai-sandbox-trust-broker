# Token Verification and Translation Service

Part of the [UIDAI Sandbox Trust Broker](../README.md).

This service is the **core trust-processing engine** of the Trust Broker. It is responsible for:

- **Token Verification**: Validating JWT/OIDC tokens via **JWKS**.
- **JWKS Caching**: Reducing latency by caching third-party signing keys in **Redis**.
- **Asynchronous Consumption**: Processing token requests from **Kafka** topics.
- **Format Translation**: Normalizing token payloads across service boundaries.
- **Explicit Signing**: Issuing a new **Sandbox Session Token** that binds normalized identity claims (e.g., `normalizedName`) to a fresh, signed JWT.

---

## Token Translation Logic

When a token is verified, the service doesn't just return the status. It "translates" the context into a new internal trust domain by:
1.  **Extracting Identity**: Pulling claims like `sub` and `name` from the original token.
2.  **Normalization**: Applying rules (e.g., uppercasing `name` to `normalizedName`).
3.  **Binding**: Adding these normalized fields as claims to a **newly signed JWT** (the `translatedToken`).
4.  **Issuance**: The resulting token is returned in the `translatedToken` field of the response, providing proof of authenticity within the sandbox.

---

## Infrastructure

This service requires [Kafka and Redis](../docs/infrastructure-management.md) to be running.
- **Redis**: Caches signing keys (JWKS) to optimize verification performance.
- **Kafka**: Consumes requests routed by the Interoperability Gateway.

---

## Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/token/health` | Service health check |
| `POST` | `/api/v1/token/verify` | Submit a token for synchronous verification |

### Example Request
```bash
curl -X POST http://localhost:8082/api/v1/token/verify \
  -H "Content-Type: application/json" \
  -d '{
    "systemId": "test-system",
    "token": "<raw-token>",
    "targetAudience": "uidai-auth"
  }'
```

### Example Response
```json
{
  "status": "VERIFIED",
  "message": "Token successfully verified and translated for test-system",
  "translatedToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6InNhbmRib3gtayJ9...",
  "details": {
    "systemId": "test-system",
    "subject": "sub-123456789",
    "trustLevel": "HIGH",
    "processedAt": "2026-04-11T11:20:46Z",
    "expiresAt": "2026-04-11T12:20:46Z"
  }
}
```

---

## Configuration

| Property | Default | Description |
|---|---|---|
| `server.port` | `8082` | HTTP listener port |
| `spring.cache.type` | `redis` | Enables JWKS caching |
| `spring.kafka.consumer.group-id` | `token-service-group` | Kafka consumer group |

Configuration lives in [`src/main/resources/application.properties`](src/main/resources/application.properties).

---

## Package Structure

```
com.uidai.sandbox.token
├── TokenVerificationApplication.java
├── controller/
│   └── TokenController.java      ← REST endpoints
├── service/
│   ├── TokenService.java         ← Verification logic
│   ├── JwksService.java          ← JWKS retrieval & caching
│   └── KafkaConsumerService.java ← Async request processor
└── config/
    ├── RedisConfig.java          ← Cache configuration
    └── SecurityConfig.java       ← JWT security setup
```

---

## Dependencies

| Dependency | Purpose |
|---|---|
| `trust-broker-common` | Shared DTOs and Kafka config |
| `spring-boot-starter-security` | JWT decoding/verification |
| `spring-boot-starter-data-redis` | JWKS caching |
| `spring-kafka` | Async request processing |
