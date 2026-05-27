# UIDAI Sandbox Trust Broker

A multi-module Spring Boot project that provides a **Trust Broker** layer for UIDAI sandbox environments. It bridges disparate identity and authentication systems by verifying, translating, and routing tokens across service boundaries.

---

## Modules

| Module | Port | Responsibility |
|---|---|---|
| [`interoperability-gateway-service`](./interoperability-gateway-service/README.md) | `8081` | Entry point for inter-system requests; routes and adapts protocol differences. Includes the **System Registry** for trust management. |
| [`token-verification-and-translation-service`](./token-verification-and-translation-service/README.md) | `8082` | Verifies token authenticity (JWT/JWKS) and translates formats between identity providers. |
| [`trust-broker-common`](./trust-broker-common/README.md) | N/A | Shared DTOs, Kafka configurations, and common error handlers. |

---

## Architecture Overview

```
External System / Client
        │
        ▼
┌─────────────────────────────────┐
│  Interoperability Gateway (8081) │  ← Protocol adaptation, routing
└──────────────┬──────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│  Token Verification & Translation (8082)  │  ← JWT/token verification, format translation
└──────────────────────────────────────────┘
               │
               ▼
       Identity Provider / UIDAI Sandbox
```

---

## Tech Stack

- **Java 25** (LTS)
- **Spring Boot 3.5.14**
- **Maven** (multi-module build)

---

## Prerequisites

| Tool | Minimum Version |
|---|---|
| JDK | 25 |
| Maven | 3.8+ |
| Docker | Desktop (latest) |

---

## Infrastructure

The project requires Kafka, Zookeeper, and Redis to be running. See the [Infrastructure Management Guide](./docs/infrastructure-management.md) for detailed setup and management instructions.

---

## Building the Project

### Build all modules from the root

```bash
mvn clean install
```

### Build a specific module

```bash
mvn clean install -pl interoperability-gateway-service
mvn clean install -pl token-verification-and-translation-service
```

### Skip tests

```bash
mvn clean install -DskipTests
```

---

## Running the Services

### Interoperability Gateway Service

```bash
cd interoperability-gateway-service
mvn spring-boot:run
```

Verify it's running:
```bash
curl http://localhost:8081/api/v1/gateway/health
```

### Token Verification & Translation Service

```bash
cd token-verification-and-translation-service
mvn spring-boot:run
```

Verify with a test token payload:
```bash
curl -X POST http://localhost:8082/api/v1/token/verify \
  -H "Content-Type: application/json" \
  -d '{"systemId": "test-system", "token": "<your-token-here>"}'
```

---

## API Documentation

All services provide interactive API documentation via Swagger UI. Once the services are running, you can access them at:

| Service | Swagger UI URL |
|---|---|
| Interoperability Gateway | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) |
| Token Verification | [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) |

---

## End-to-End Validation

The project includes an E2E validation script that tests the entire flow from system registration to token verification.

### Run E2E Tests

Ensure all services (Kafka, Redis, and Java Apps) are running, then:

```bash
chmod +x e2e_test.sh
./e2e_test.sh
```

This script validates:
1.  **System Registration**: Adding external systems to the central registry.
2.  **Routing Rules**: Defining Kafka-based routing topics dynamically.
3.  **Gateway Dispatch**: Sending a token request through the gateway.
4.  **Verification Flow**: Full vertical verification across services.

---

## Running Tests

```bash
# All modules
mvn test

# Single module
mvn test -pl interoperability-gateway-service
```

### Validation Reports
Detailed implementation and testing reports are available in the [`docs/`](./docs/) directory:
- [Roadmap Validation Report](./docs/roadmap-validation-report.md)
- [Architecture Blueprint](./docs/blueprints/architecture-blueprint.md)

---

## Project Structure

```
uidai-sandbox-trust-broker/
├── pom.xml                                        # Parent POM
├── README.md                                      # Project Hub
├── docs/                                          # PROJECT BLUEPRINTS & Design Docs
│   ├── blueprints/
│   │   ├── architecture-blueprint.md
│   │   └── testing-blueprint.md
│   └── infrastructure-management.md               # Docker infrastructure guide
├── docker/                                        # Kafka, Redis, ZooKeeper configs
├── interoperability-gateway-service/              # Entry point & Routing
│   ├── pom.xml
│   └── src/main/java/com/uidai/sandbox/gateway/
├── token-verification-and-translation-service/    # Verification & Security Hub
│   ├── pom.xml
│   └── src/main/java/com/uidai/sandbox/token/
└── trust-broker-common/                           # Shared DTOs & Models
    ├── pom.xml
    └── src/main/java/com/uidai/sandbox/common/
```

---

## Contributing

1. Fork the repository and create a feature branch from `main`.
2. Follow standard Java/Spring Boot conventions (package by feature, not layer, where appropriate).
3. Ensure all tests pass before raising a pull request.
4. Update the relevant module `README.md` when adding new endpoints or configuration.

---

## License

Internal use — UIDAI Sandbox. Not for public distribution.
