# Skill: Roadmap Implementation Validation

## Context
Use this skill when you need to audit a project's current implementation against an established blueprint or roadmap document (e.g., `architecture-blueprint.md` or `project-roadmap.md`).

## Instructions

### 1. Locate the Source of Truth
Find the primary blueprint or roadmap file in the repository (usually under `docs/` or `docs/plan/`). Identifiers include headers like "Implementation Roadmap", "Phases", or "Architecture Overview".

### 2. Service & Module Inventory
List all directories in the project root to identify active services and shared libraries.
- Compare these against the "Core Modules" section of the blueprint.
- Verify if the `pom.xml` or build files include the expected modules.

### 3. Progressive Phase Audit
Review each phase of the roadmap sequentially and perform the following checks:

#### Phase: Baseline / Foundation
- Check for the existence of service entry points (e.g., Spring Boot `@SpringBootApplication`).
- Verify health check endpoints (e.g., `/health`) in Controllers.
- Check for OpenAPI/Swagger configuration.

#### Phase: Shared Libraries & DTOs
- Inspect shared modules (e.g., `common`, `sdk`) for DTOs.
- Check if core services import and use these shared DTOs in their controllers.
- Check for global exception handlers and standard error formats.

#### Phase: Infrastructure Integration
- Inspect `docker-compose.yml` for required services (Kafka, Redis, Postgres).
- Check `src/main/resources/application.yml` for connection strings.
- Search for implementation logic:
    - **Kafka**: Search for `@KafkaListener`, `KafkaTemplate`, or `NewTopic` beans.
    - **Redis**: Search for `RedisTemplate`, `@Cacheable`, or Redis dependencies.

#### Phase: Core Business Logic
- Check services and controllers for `TODO` comments or stubbed logic.
- Verify logic for key components (e.g., token translation, signature verification).

### 4. Reporting & Synchronization

Execute the following updates to ensure documentation remains in sync with the audit findings:

#### A. Update Source Roadmap
Update the primary blueprint or roadmap file (`docs/plan/uidai-sandbox-trust-broker-blueprint.md` or similar) to synchronize status tags:
- Change `(Next)` to `(In-Progress)` or `(Complete)` as appropriate.
- Add implementation notes (e.g., "(Implemented)", "(Partially Started)").

#### B. Generate/Update Validation Report
Update the detailed report (e.g., `docs/roadmap-validation-report.md`) including:
- **Audit Metadata**: A `Last Validated` date and audit status.
- **Completion Percentage** per phase.
- **Detailed Checkboxes**: Checklist of implemented vs missing features.
- **Gaps & Sloped Logic**: Explicitly list stubbed services or missing configuration.
- **Recommended Next Steps**: Prioritized list of tasks to move the roadmap forward.
