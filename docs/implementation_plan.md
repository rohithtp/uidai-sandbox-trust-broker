# JDK 25 LTS Upgrade — UIDAI Sandbox Trust Broker

Upgrade the project from **Java 17 / Spring Boot 3.2.0** to **Java 25 LTS / Spring Boot 3.5.14**, modernise the codebase to leverage new JDK features, and update project documentation with a new roadmap phase.

## Current State

| Component | Current Version |
|---|---|
| JDK | 17 |
| Spring Boot | 3.2.0 |
| Maven Compiler Plugin | 3.11.0 |
| Lombok | 1.18.44 |
| nimbus-jose-jwt | 9.37.3 |
| springdoc-openapi | 2.3.0 |



## Proposed Changes

### Phase A: Build & Dependency Upgrades

---

#### [MODIFY] [pom.xml](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/pom.xml)

Update root POM properties and plugin versions:

```diff
 <properties>
-    <maven.compiler.source>17</maven.compiler.source>
-    <maven.compiler.target>17</maven.compiler.target>
+    <maven.compiler.source>25</maven.compiler.source>
+    <maven.compiler.target>25</maven.compiler.target>
     <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
-    <spring-boot.version>3.2.0</spring-boot.version>
+    <spring-boot.version>3.5.14</spring-boot.version>
+    <lombok.version>1.18.46</lombok.version>
+    <springdoc.version>3.0.3</springdoc.version>
 </properties>
```

Update `maven-compiler-plugin`:
```diff
-    <version>3.11.0</version>
+    <version>3.15.0</version>
     ...
-        <version>1.18.44</version>
+        <version>${lombok.version}</version>
```

---

#### [MODIFY] [interoperability-gateway-service/pom.xml](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/interoperability-gateway-service/pom.xml)

- Lombok version → `${lombok.version}` (inherited from parent)
- springdoc version → `${springdoc.version}`

---

#### [MODIFY] [token-verification-and-translation-service/pom.xml](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/token-verification-and-translation-service/pom.xml)

- Lombok version → `${lombok.version}`
- springdoc version → `${springdoc.version}`

---

#### [MODIFY] [trust-broker-common/pom.xml](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/trust-broker-common/pom.xml)

- Lombok version → `${lombok.version}`
- springdoc version → `${springdoc.version}`
- nimbus-jose-jwt: `9.37.3` → `10.9` (fixes CVE-2025-53864)

---

### Phase B: Java Language Modernisation

These are the JDK 17→25 features that directly align with patterns already in the codebase:

---

#### B1. Convert Immutable DTOs to Java Records

**Feature**: Records (Java 16+, stable) — eliminates boilerplate constructors, getters, `equals`, `hashCode`, `toString`.

**Affected files:**

##### [MODIFY] [TokenRequest.java](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/trust-broker-common/src/main/java/com/uidai/sandbox/common/dto/TokenRequest.java)

```diff
-@Data @Builder @NoArgsConstructor @AllArgsConstructor
-public class TokenRequest {
-    private String token;
-    private String systemId;
-}
+public record TokenRequest(String token, String systemId) {}
```

> [!NOTE]
> This changes the API from `.getToken()` to `.token()`. All call-sites (GatewayServiceImpl, KafkaProducerServiceImpl, KafkaConsumerService, TokenServiceImpl, controllers, tests) will be updated.

##### [MODIFY] [TokenResponse.java](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/trust-broker-common/src/main/java/com/uidai/sandbox/common/dto/TokenResponse.java)

```diff
-@Data @Builder @NoArgsConstructor @AllArgsConstructor
-public class TokenResponse {
-    private String status;
-    private String message;
-    private String translatedToken;
-    private Map<String, Object> details;
-}
+public record TokenResponse(
+    String status,
+    String message,
+    String translatedToken,
+    Map<String, Object> details
+) {}
```

> All `TokenResponse.builder()` call-sites will be replaced with `new TokenResponse(...)` constructor calls.

##### [MODIFY] [ErrorResponse.java](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/trust-broker-common/src/main/java/com/uidai/sandbox/common/dto/ErrorResponse.java)

```diff
-@Data @Builder @NoArgsConstructor @AllArgsConstructor
-public class ErrorResponse {
-    private String message;
-    private String errorCode;
-    private LocalDateTime timestamp;
-    private String path;
-}
+public record ErrorResponse(
+    String message,
+    String errorCode,
+    LocalDateTime timestamp,
+    String path
+) {}
```

##### [MODIFY] [AuditEvent.java](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/trust-broker-common/src/main/java/com/uidai/sandbox/common/dto/AuditEvent.java)

```diff
-@Data @Builder @NoArgsConstructor @AllArgsConstructor
-public class AuditEvent { ... }
+public record AuditEvent(
+    String eventId, String serviceName, String action,
+    String status, String actor, LocalDateTime timestamp,
+    String metadata
+) {}
```

##### `ExternalSystem` and `RoutingRule` — **Keep as Lombok classes**
These implement `Serializable` and use mutable state (`system.setTrustLevel(...)`). Converting them to records would require a different pattern (immutable + `with*` methods). Deferred to a follow-up.

---

#### B2. Sealed Interface for Service Responses

**Feature**: Sealed classes/interfaces (Java 17+, stable) — restrict subtypes for exhaustive pattern matching.

##### [NEW] [VerificationResult.java](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/trust-broker-common/src/main/java/com/uidai/sandbox/common/dto/VerificationResult.java)

Introduce a sealed result type for token verification outcomes:

```java
public sealed interface VerificationResult {
    record Success(String subject, String sessionToken, Map<String, Object> details) 
        implements VerificationResult {}
    record Failure(String reason, String systemId) 
        implements VerificationResult {}
}
```

This pairs with pattern matching in `switch` (B3) to replace the current stringly-typed status checks.

---

#### B3. Pattern Matching for `switch` & Record Patterns

**Feature**: Pattern matching for switch (Java 21+, stable) + Record patterns (Java 21+, stable)

##### [MODIFY] [GatewayServiceImpl.java](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/interoperability-gateway-service/src/main/java/com/uidai/sandbox/gateway/service/impl/GatewayServiceImpl.java)

Replace the `if-else` trust level validation chain with pattern matching:

```java
// Before (stringly typed)
if (system.getTrustLevel() == null || system.getTrustLevel() == TrustLevel.LOW) { ... }

// After (pattern matching switch with guard)
return switch (system.getTrustLevel()) {
    case null, LOW -> buildRejectedResponse("Insufficient trust level", request.systemId());
    case MEDIUM, HIGH, CRITICAL -> { /* proceed with routing */ }
};
```

##### [MODIFY] [TokenServiceImpl.java](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/token-verification-and-translation-service/src/main/java/com/uidai/sandbox/token/service/impl/TokenServiceImpl.java)

If we adopt the `VerificationResult` sealed interface, the consumer side can use exhaustive record-pattern matching:

```java
return switch (tokenService.verifyAndTranslate(request)) {
    case VerificationResult.Success(var subject, var token, var details) -> 
        ResponseEntity.ok(new TokenResponse("VERIFIED", ...));
    case VerificationResult.Failure(var reason, var systemId) -> 
        ResponseEntity.status(401).body(new TokenResponse("FAILED", reason, null, ...));
};
```

---

#### B4. Virtual Threads (Project Loom)

**Feature**: Virtual threads (Java 21+, stable) — lightweight, high-throughput concurrency.

This is a **huge win** for this project since both services are heavily I/O-bound (Redis lookups, Kafka messaging, external JWKS fetching).

##### [MODIFY] [application.properties](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/interoperability-gateway-service/src/main/resources/application.properties)

```diff
+# Enable Virtual Threads (JDK 21+)
+spring.threads.virtual.enabled=true
```

##### [MODIFY] [application.properties](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/token-verification-and-translation-service/src/main/resources/application.properties)

```diff
+# Enable Virtual Threads (JDK 21+)
+spring.threads.virtual.enabled=true
```

> [!NOTE]
> With virtual threads enabled by default, Spring Boot automatically configures Tomcat, `@Async`, `@Scheduled`, and Kafka listeners to use virtual threads. No code changes needed. It can be opted-out by setting `spring.threads.virtual.enabled=false` via properties or environment variables (`SPRING_THREADS_VIRTUAL_ENABLED=false`).

---

#### B5. Sequenced Collections

**Feature**: Sequenced Collections (Java 21+, stable)

##### [MODIFY] [GatewayServiceImpl.java](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/interoperability-gateway-service/src/main/java/com/uidai/sandbox/gateway/service/impl/GatewayServiceImpl.java)

Replace the stream-sort-findFirst pattern for picking the highest-priority routing rule:

```diff
-var selectedRule = routingRules.stream()
-    .sorted((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()))
-    .findFirst()
-    .orElseThrow();
+// Pre-sort the list once, then use SequencedCollection.getFirst()
+routingRules.sort(Comparator.comparingInt(RoutingRule::getPriority).reversed());
+var selectedRule = routingRules.getFirst();
```

---

#### B6. Flexible Constructor Bodies

**Feature**: Flexible Constructor Bodies (Java 25, stable) — allows pre-validation logic before `super()`/`this()`.

##### [MODIFY] [SecurityConfig.java (token-service)](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/token-verification-and-translation-service/src/main/java/com/uidai/sandbox/token/config/SecurityConfig.java)

The current constructor can validate `JwksService` is non-null before assignment:

```java
public SecurityConfig(JwksService jwksService) {
    // JDK 25: validation before field assignment (flexible constructor)
    if (jwksService == null) {
        throw new IllegalArgumentException("JwksService must not be null");
    }
    this.jwksService = jwksService;
}
```

*(Minor improvement — shows awareness of the feature. Larger benefit comes in deeper class hierarchies.)*

---

#### B7. Scoped Values (JDK 25, stable)

**Feature**: Scoped Values (JEP 506) — safer, more efficient alternative to `ThreadLocal` for sharing immutable request-scoped data.

##### [NEW] [RequestContext.java](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/trust-broker-common/src/main/java/com/uidai/sandbox/common/context/RequestContext.java)

Introduce scoped values for propagating request context (system ID, correlation ID) through the call chain without explicit parameter threading:

```java
public final class RequestContext {
    public static final ScopedValue<String> SYSTEM_ID = ScopedValue.newInstance();
    public static final ScopedValue<String> CORRELATION_ID = ScopedValue.newInstance();

    private RequestContext() {}
}
```

Usage in GatewayController:

```java
ScopedValue.where(RequestContext.SYSTEM_ID, request.systemId())
    .where(RequestContext.CORRELATION_ID, UUID.randomUUID().toString())
    .run(() -> gatewayService.processIncomingRequest(request));
```

---

#### B8. Key Derivation Function (KDF) API

**Feature**: KDF API (JEP 510, Java 25) — standardised key derivation for cryptographic operations.

##### [MODIFY] [SecurityConfig.java (token-service)](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/token-verification-and-translation-service/src/main/java/com/uidai/sandbox/token/config/SecurityConfig.java)

Document and optionally integrate the KDF API for future HKDF-based key derivation when issuing sandbox session tokens. This is a forward-looking alignment — the current RSA key pair generation remains unchanged, but a TODO/comment is added for future integration.

---

### Phase C: Docker & Infrastructure

---

#### [MODIFY] [docker-compose.yml](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/docker/docker-compose.yml)

Update docker-compose version and add JDK version comment:

```diff
-version: '3.8'
+# version key removed (deprecated in Compose V2)
```

*(No JDK base image Dockerfile exists yet — add one if needed for containerised builds.)*

---

### Phase D: Documentation Updates

---

#### [MODIFY] [README.md](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/README.md)

Update the Tech Stack and Prerequisites sections:

```diff
 ## Tech Stack
-
-- **Java 17**
-- **Spring Boot 3.2.0**
+- **Java 25** (LTS)
+- **Spring Boot 3.5.14**
 - **Maven** (multi-module build)

 ## Prerequisites
 | Tool | Minimum Version |
 |---|---|
-| JDK | 17 |
+| JDK | 25 |
 | Maven | 3.8+ |
 | Docker | Desktop (latest) |
```

---

#### [MODIFY] [uidai-sandbox-trust-broker-blueprint.md](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/docs/plan/uidai-sandbox-trust-broker-blueprint.md)

Add new roadmap phases for the JDK upgrade and JDK-aligned features:

```diff
 ## 5. Implementation Roadmap

 1. **Phase 1 (Complete)**: Baseline services with health checks and basic REST controllers.
 2. **Phase 2 (Complete)**: Establishment of `trust-broker-common` and shared DTO normalization.
 3. **Phase 3 (Complete)**: Integration of Kafka for event flow and Redis for JWKS caching.
 4. **Phase 4 (Complete)**: Implementation of the **Centralized Authentication Broker** logic.
 5. **Phase 5 (Complete)**: Full lifecycle validation, including universal Maven build verification and E2E flow testing.
+6. **Phase 6 (Planned)**: **JDK 25 LTS Platform Upgrade** — Upgrade build toolchain to Java 25, Spring Boot 3.5.14, and all dependencies. Enable Virtual Threads for I/O-bound services. Update Docker base images.
+7. **Phase 7 (Planned)**: **JDK 25 Language Modernisation** — Convert immutable DTOs to Java Records. Introduce sealed interfaces for typed verification results. Adopt pattern-matching switch. Integrate Scoped Values for request context. Leverage Sequenced Collections and Flexible Constructor Bodies.
```

---

#### [MODIFY] [architecture-blueprint.md](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/docs/blueprints/architecture-blueprint.md)

Add a "Platform & Runtime" section documenting JDK 25 features in use:

```diff
+## Platform & Runtime
+- **JDK**: 25 LTS (Virtual Threads, Records, Pattern Matching, Sealed Interfaces, Scoped Values)
+- **Spring Boot**: 3.5.14 (virtual thread auto-configuration, improved observability)
+- **Concurrency Model**: Virtual threads (`spring.threads.virtual.enabled=true`) for all I/O-bound request handling
```

---

#### [MODIFY] [roadmap-validation-report.md](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/docs/roadmap-validation-report.md)

Add Phase 6 & Phase 7 rows to the summary table and detail sections.

---

## Dependency Version Summary (After Upgrade)

| Component | Before | After |
|---|---|---|
| JDK | 17 | **25** (LTS) |
| Spring Boot | 3.2.0 | **3.5.14** |
| Maven Compiler Plugin | 3.11.0 | **3.15.0** |
| Lombok | 1.18.44 | **1.18.46** |
| nimbus-jose-jwt | 9.37.3 | **10.9** |
| springdoc-openapi | 2.3.0 | **3.0.3** |
| maven-surefire-report-plugin | 3.5.5 | 3.5.5 (unchanged) |

---

## JDK Features → Codebase Alignment Summary

| JDK Feature | Version | Where It Applies |
|---|---|---|
| **Records** | 16+ (stable) | `TokenRequest`, `TokenResponse`, `ErrorResponse`, `AuditEvent` DTOs |
| **Sealed Interfaces** | 17+ (stable) | New `VerificationResult` sealed interface for typed outcomes |
| **Pattern Matching switch** | 21+ (stable) | Trust level validation in `GatewayServiceImpl`, response handling |
| **Virtual Threads** | 21+ (stable) | Both services — Tomcat, Kafka listeners, `@Async`, `@Scheduled` |
| **Sequenced Collections** | 21+ (stable) | Routing rule priority selection in `GatewayServiceImpl` |
| **Scoped Values** | 25 (stable) | Request context propagation (`RequestContext.SYSTEM_ID`, `CORRELATION_ID`) |
| **Flexible Constructor Bodies** | 25 (stable) | Pre-validation in `SecurityConfig`, applicable to any constructor chain |
| **KDF API** | 25 (stable) | Future: Key derivation for session token generation |
| **Compact Object Headers** | 25 (JVM-level) | Automatic — reduced memory per object, benefits Redis-heavy caching workloads |
| **Generational ZGC** | 25 (JVM-level) | Automatic — improved GC for long-running services |

---

## Verification Plan

### Automated Tests

1. **Full Maven Build**: `mvn clean verify` from root — ensures all 3 modules compile against JDK 25 and all tests pass.
2. **Unit Tests**: `mvn test` — especially the record DTO serialization/deserialization tests (Kafka JSON, Redis, Jackson).
3. **E2E Flow**: Run `./e2e_test.sh` with both services on the new JDK to validate the full vertical (Registry → Gateway → Kafka → Token Service).

### Manual Verification

1. **Swagger UI**: Confirm both services still render OpenAPI docs at their respective URLs after springdoc upgrade to 3.0.3.
2. **Virtual Thread Verification**: Check thread names in logs — virtual thread names follow `VirtualThread[#N]` pattern instead of `http-nio-808x-exec-N`.
3. **Dependency Security**: Run `mvn dependency:tree` and verify nimbus-jose-jwt 10.9 resolves (CVE-2025-53864 patched).
