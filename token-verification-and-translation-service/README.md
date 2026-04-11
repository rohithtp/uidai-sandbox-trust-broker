# Token Verification and Translation Service

Part of the [UIDAI Sandbox Trust Broker](../README.md).

This service is the **core trust-processing engine** of the Trust Broker. It is responsible for:

- **Token Verification**: Validating JWT/OIDC tokens via **JWKS**.
- **JWKS Caching**: Reducing latency by caching third-party signing keys in **Redis**.
- **Asynchronous Consumption**: Processing token requests from **Kafka** topics.
- **Format Translation**: Normalizing token payloads across service boundaries.

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
