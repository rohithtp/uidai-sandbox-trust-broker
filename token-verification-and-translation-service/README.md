# Token Verification and Translation Service

Part of the [UIDAI Sandbox Trust Broker](../README.md).

This service is the **core trust-processing engine** of the Trust Broker. It is responsible for:

- **Verifying** the authenticity and integrity of tokens (e.g., JWT, OIDC tokens, UIDAI-specific formats)
- **Translating** tokens between different identity provider formats
- Enforcing security policies via Spring Security

---

## Prerequisites

This service requires [Kafka and Redis](../docs/infrastructure-management.md) to be running.

## Endpoints

| Method | Path | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/v1/token/verify` | Submit a token for verification and translation | TBD |

### Example Request

```bash
curl -X POST http://localhost:8082/api/v1/token/verify \
  -H "Content-Type: application/json" \
  -d '{"token": "<raw-token>"}'
```

### Example Response *(stub — not yet implemented)*

```json
{
  "service": "token-verification-and-translation-service",
  "status": "NOT_IMPLEMENTED",
  "received": {
    "token": "<raw-token>"
  }
}
```

---

## Configuration

| Property | Default | Description |
|---|---|---|
| `server.port` | `8082` | HTTP listener port |
| `spring.application.name` | `token-verification-and-translation-service` | Service name |

Configuration lives in [`src/main/resources/application.properties`](src/main/resources/application.properties).

> **Note:** Spring Security is on the classpath. By default it will auto-generate a password and require HTTP Basic auth on all endpoints. Configure `application.properties` or add a `SecurityConfig` class to relax/restrict access as needed.

---

## Running Locally

From the module root:

```bash
mvn spring-boot:run
```

Or from the project root:

```bash
mvn spring-boot:run -pl token-verification-and-translation-service
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
| `spring-boot-starter-security` | Authentication and authorization framework |
| `spring-boot-starter-json` | Jackson JSON serialization/deserialization |
| `spring-boot-starter-test` | JUnit 5, Mockito, MockMvc |
| `spring-security-test` | Security context helpers for tests |

---

## Package Structure

```
com.uidai.sandbox.token
├── TokenVerificationApplication.java   ← Spring Boot entry point
└── controller/
    └── TokenController.java            ← REST controllers
```

> Recommended growth structure:
> ```
> com.uidai.sandbox.token
> ├── controller/     ← REST layer
> ├── service/        ← Verification and translation business logic
> ├── model/          ← Request/response DTOs
> ├── config/         ← SecurityConfig, etc.
> └── exception/      ← Custom exceptions and error handlers
> ```

---

## Implementation Guide

### 1. Token Verification
Implement signature verification in a `service/TokenVerificationService.java`. For JWT tokens, consider using:
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
```

### 2. Token Translation
Implement format conversion (e.g., OIDC → SAML, UIDAI token → standard JWT) in a `service/TokenTranslationService.java`.

### 3. Security Configuration
Add a `config/SecurityConfig.java` to configure which endpoints require authentication and which are public.

---

## Extension Points

- **Add JWT library** (`jjwt` or `nimbus-jose-jwt`) for full JWT verification.
- **Add JWKS endpoint support** to fetch signing keys dynamically from an identity provider.
- **Add Spring Actuator** for production-grade observability.
