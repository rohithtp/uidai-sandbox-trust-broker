# Interoperability Gateway Service

Part of the [UIDAI Sandbox Trust Broker](../README.md).

This service acts as the **entry point** for external systems interacting with the Trust Broker. It is responsible for:

- **Auth Handshake**: Validating external systems via the **System Registry**.
- **Dynamic Routing**: Dispatched requests to Kafka topics based on registry-defined rules.
- **Protocol Adaptation**: Abstracting protocol and format differences from callers.

---

## Infrastructure

This service requires [Kafka and Redis](../docs/infrastructure-management.md) to be running.
- **Redis**: Stores the System Registry (External Systems and Routing Rules).
- **Kafka**: Destinations for routed token requests.

---

## Endpoints

### Gateway API
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/gateway/health` | Service health check |
| `POST` | `/api/v1/gateway/process` | Accept and route a token request |

### System Registry API (Management)
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/registry/systems` | Register a new external system |
| `GET` | `/api/v1/registry/systems` | List all registered systems |
| `GET` | `/api/v1/registry/systems/{id}` | Get metadata for a specific system |
| `PATCH` | `/api/v1/registry/systems/{id}/trust` | Update system trust level |
| `POST` | `/api/v1/registry/rules` | Add a dynamic routing rule |
| `GET` | `/api/v1/registry/systems/{id}/rules` | List rules for a system |
| `DELETE` | `/api/v1/registry/rules/{id}` | Remove a routing rule |

---

## Configuration

| Property | Default | Description |
|---|---|---|
| `server.port` | `8081` | HTTP listener port |
| `spring.data.redis.host` | `localhost` | Redis host for registry storage |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker address |

Configuration lives in [`src/main/resources/application.properties`](src/main/resources/application.properties).

---

## Running Locally

From the module root:
```bash
mvn spring-boot:run
```

### Health check:
```bash
curl http://localhost:8081/api/v1/gateway/health
```

### Process Request (Example):
```bash
curl -X POST http://localhost:8081/api/v1/gateway/process \
  -H "Content-Type: application/json" \
  -d '{
    "systemId": "test-system",
    "token": "raw.jwt.token",
    "targetAudience": "uidai-auth"
  }'
```

---

## Package Structure

```
com.uidai.sandbox.gateway
├── InteroperabilityGatewayApplication.java
├── controller/
│   ├── GatewayController.java         ← Entry point routing
│   └── SystemRegistryController.java  ← Management APIs
├── service/
│   ├── GatewayService.java            ← Dispatch logic
│   └── SystemRegistryService.java     ← Redis-backed registry
└── config/
    └── RedisConfig.java               ← Registry persistence config
```

---

## Dependencies

| Dependency | Purpose |
|---|---|
| `trust-broker-common` | Shared DTOs and Kafka config |
| `spring-boot-starter-data-redis` | Registry persistence |
| `spring-kafka` | Event-driven dispatch |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI documentation |
