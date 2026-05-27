---
name: uidai-project-context
description: Use this skill to understand the project architecture, blueprints, and AI agent guidelines for the UIDAI Sandbox Trust Broker. Trigger this when you need context about the project's structure, before making architectural changes, or when creating new features.
---

# UIDAI Sandbox Trust Broker Context

You are working on the **UIDAI Sandbox Trust Broker** repository.

## Documentation Structure
The project documentation has been organized into the following directories. Use the `view_file` tool to read these when you need specific information:

1. **`docs/agents.md`**: Central AI Agent guidelines, coding standards (Java 25, Spring Boot 3.5.x, Kafka, Redis), and core module descriptions. **Read this first** if you are modifying core logic.
2. **`docs/blueprints/`**: Contains architectural and testing blueprints (e.g., `uidai-sandbox-trust-broker-blueprint.md`, `architecture-blueprint.md`).
3. **`docs/plans/`**: Contains execution and migration plans (e.g., `implementation_plan.md`, `kubernetes_migration_plan.md`).
4. **`docs/analysis/`**: Contains system analysis and answers to model questions.
5. **`docs/infrastructure/`**: Contains infrastructure management guides.
6. **`docs/testing/`**: Contains End-to-End (E2E) testing flows.

## Actionable Guidelines
- When the user asks you to implement a new feature, verify if it aligns with the blueprints in `docs/blueprints/`.
- Ensure all Java code you write conforms to the Java 25 modernization rules outlined in `docs/agents.md` (Records, pattern matching, virtual threads).
- Follow the API-First and Event-Driven principles documented for this microservices architecture.
