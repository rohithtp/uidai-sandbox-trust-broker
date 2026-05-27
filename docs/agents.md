# AI Agent Guidelines (agents.md)

This document provides context and rules for AI coding agents and assistants working on the `uidai-sandbox-trust-broker` project.

## 1. Project Context
The **UIDAI Sandbox Trust Broker** is a middleware system that acts as a secure, event-driven gateway between external consumer systems and internal sandbox services.
It uses a **Microservices Architectural Pattern** within a Maven Multi-Module repository.

## 2. Technology Stack
- **Java Version:** JDK 25 (LTS)
- **Framework:** Spring Boot 3.5.x
- **Event Streaming:** Apache Kafka
- **Caching:** Redis
- **Build Tool:** Maven

## 3. Core Modules
- `interoperability-gateway-service`: The entry point for external systems. Handles routing, validation, and event publishing to Kafka.
- `token-verification-and-translation-service`: The security hub validating and translating JWTs asynchronously.
- `trust-broker-common`: Shared libraries, DTOs, custom exceptions, and utility classes.

## 4. Coding Standards & Guidelines for AI Agents

### 4.1 Java 25 Modernization
Agents must write code leveraging the latest Java 25 features:
- Use **Records** for immutable DTOs instead of standard POJOs with getters/setters.
- Use **Sealed Interfaces/Classes** where domain hierarchies are restricted.
- Use **Pattern Matching for switch** and instance-of.
- Utilize **Virtual Threads** for I/O-bound operations if applicable.
- Leverage **Sequenced Collections** and flexible constructor bodies where appropriate.

### 4.2 Architecture & Design
- **API First:** Always update or refer to OpenAPI (Swagger) documentation when modifying REST controllers.
- **Event-Driven:** Favor asynchronous event publishing via Kafka over synchronous HTTP calls between microservices.
- **Stateless Services:** Ensure business logic services remain stateless to allow horizontal scaling.
- **Package Structure:** Adhere to the established package layout (`.config`, `.controller`, `.service`, `.messaging`, `.model.dto`, `.exception`, `.client`).

### 4.3 Testing
- Write Unit tests for all business logic (`.service`).
- Include Integration tests for Kafka messaging and Redis caching layers.
- Avoid modifying the core End-to-End (`e2e_test.sh`) script unless explicitly required.

### 4.4 Documentation
- Update relevant markdown files in the `docs/` directory when altering architectural decisions or plans.
- Provide descriptive commit messages or artifact summaries explaining *why* a change was made.
