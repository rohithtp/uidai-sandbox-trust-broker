# Interoperability Gateway Service

Part of the [UIDAI Sandbox Trust Broker](../README.md).

This service acts as the **entry point** for external systems interacting with the Trust Broker. It is responsible for:

- Accepting inbound requests from heterogeneous systems
- Adapting and routing requests to downstream Trust Broker services
- Abstracting protocol and format differences from callers

---

## Prerequisites

This service requires [Kafka and Redis](../docs/infrastructure-management.md) to be running.

## Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/gateway/health` | Service health check |

> Additional routing and gateway endpoints will be added here as the service evolves.

---

## Configuration

| Property | Default | Description |
|---|---|---|
| `server.port` | `8081` | HTTP listener port |
| `spring.application.name` | `interoperability-gateway-service` | Service name (used in logs and tracing) |

Configuration lives in [`src/main/resources/application.properties`](src/main/resources/application.properties).

---

## Running Locally

From the module root:

```bash
mvn spring-boot:run
```

Or from the project root:

```bash
mvn spring-boot:run -pl interoperability-gateway-service
```

Health check:
```bash
curl http://localhost:8081/api/v1/gateway/health
# → {"service":"interoperability-gateway-service","status":"UP"}
```

---

## Running Tests

```bash
mvn test
```

---

## Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST API support (embedded Tomcat) |
| `spring-boot-starter-test` | JUnit 5, Mockito, MockMvc |

---

## Package Structure

```
com.uidai.sandbox.gateway
├── InteroperabilityGatewayApplication.java   ← Spring Boot entry point
└── controller/
    └── GatewayController.java                ← REST controllers
```

> Follow a **package-by-feature** layout as the service grows (e.g., `routing/`, `adapter/`, `config/`).

---

## Extension Points

- **Add routing logic** in a new `routing/` package to forward requests to the token service or other backends.
- **Add filters/interceptors** in a `filter/` package for request logging, rate limiting, or header validation.
- **Add Spring Actuator** for production-grade health, metrics, and info endpoints:
  ```xml
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
  </dependency>
  ```
