# Framework & Native Image Feasibility Analysis

This document analyzes the feasibility, effort, and trade-offs of migrating the `uidai-sandbox-trust-broker` services from a standard Spring Boot JVM deployment to either **Spring Boot with GraalVM Native Image** or a complete rewrite to **Micronaut**.

## Feasibility Assessment

**Status: Highly Feasible for both routes, but with vastly different effort levels.**

1.  **Spring Boot 3.5 Support**: The project is already on Spring Boot 3.5.x, which has first-class, out-of-the-box support for GraalVM native images using Spring AOT (Ahead-Of-Time) compilation.
2.  **Dependencies**: The current dependencies (`spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-data-redis`, `spring-boot-starter-oauth2-resource-server`, and `springdoc-openapi`) are all heavily optimized for GraalVM and should compile natively without issues.
3.  **Java Version Caveat**: The project is configured to use **JDK 25**. GraalVM native image support often stabilizes first on LTS versions (like JDK 21). While GraalVM for JDK 25 might be available (or early-access), you may encounter experimental bugs. If native compilation fails, we may need to temporarily use JDK 21/23/24 for native builds.

## Trade-offs Analysis

We evaluate three architectural approaches:

1.  **Current Implementation**: Spring Boot running on standard JVM (JIT compiler).
2.  **Spring Boot + GraalVM**: Keeping the current code, but compiling it to a Native Image.
3.  **Micronaut + GraalVM**: Rewriting the application using the Micronaut framework and compiling it to a Native Image.

| Feature | Current Implementation (Spring Boot + JVM) | Spring Boot + GraalVM Native Image | Micronaut + GraalVM Native Image |
| :--- | :--- | :--- | :--- |
| **Migration Effort** | **None**. Already implemented. | **Low**. Mostly `pom.xml` build changes. Code remains the same. | **High**. Requires rewriting annotations, security configs, and swapping libraries. |
| **Execution / Compilation** | Just-In-Time (JIT) compilation at runtime. Heavy reflection usage. | Spring AOT parses the app at build time to generate proxies and reflection hints. | Pure compile-time annotation processors. Zero runtime reflection by design. |
| **Memory Footprint** | **High** (Standard JVM footprint, typically 250MB+). | **Low** (typically 50-80MB). | **Ultra-Low** (often 20-50MB, sometimes lower than Spring Native). |
| **Startup Time** | **Slow** (typically 1-3 seconds). | **Fast** (~50-100ms). | **Blazing Fast** (~20-50ms). |
| **Throughput / Peak Perf** | **Highest** (JIT compiler optimizes aggressively over time based on actual traffic). | **Good** (AOT misses out on some runtime optimizations). | **Good** (Similar to Spring Native, AOT constrained). |
| **Framework Familiarity**| High (Standard Spring ecosystem). | High (Standard Spring ecosystem). | Medium (uses standard Jakarta EE annotations like `@Inject`, but different config paradigms). |
| **Build Time** | Very fast (seconds). | **Very Slow** (minutes, requires heavy RAM during build). | **Very Slow** (minutes, requires heavy RAM during build). |

### What a Micronaut Migration entails:
If we choose the Micronaut route to maximize startup time and minimize memory usage, we would have to replace the Spring ecosystem with Micronaut equivalents:
1.  **Annotations:** Swap `@RestController`, `@Service`, `@Autowired` for `@Controller`, `@Singleton`, `@Inject` (Jakarta EE standard).
2.  **Security:** Replace Spring Security configs (`SecurityFilterChain`) with Micronaut Security (`@Secured` and `application.yml` definitions).
3.  **Kafka:** Replace Spring Kafka (`@KafkaListener`, `KafkaTemplate`) with Micronaut Kafka (`@KafkaListener`, `@KafkaClient`).
4.  **Redis:** Replace `RedisTemplate` with Micronaut's Redis (Lettuce) integration.
5.  **Build:** Swap the Spring Boot Maven plugin for the Micronaut Maven plugin.

## Proposed Spring Boot Native Implementation Plan

If we decide to stay with Spring Boot and simply add GraalVM Native Image support (the lowest effort path for performance gains):

### Root POM Configuration (`pom.xml`)
- Introduce a `<profiles>` section with a `native` profile.
- Inside the `native` profile, configure the `spring-boot-maven-plugin` to execute the `process-aot` goal.
- Inside the `native` profile, add the `org.graalvm.buildtools:native-maven-plugin` and configure it to execute `compile-no-fork` during the `package` phase.

### Service Modules
The child modules (`interoperability-gateway-service` and `token-verification-and-translation-service`) already have the `spring-boot-maven-plugin` declared. They will automatically leverage the `native` profile from the parent POM when building with `-Pnative`.

## Verification Plan

### Automated Build Verification
To verify the configuration, we can run the native compilation for one of the services:
```bash
./mvnw -Pnative native:compile -pl interoperability-gateway-service
```
*Note: This requires a GraalVM distribution installed locally.*

### Containerized Native Image Verification
Alternatively, Spring Boot can use Cloud Native Buildpacks to generate a Docker image containing the native executable without needing GraalVM installed locally:
```bash
./mvnw -Pnative spring-boot:build-image -pl interoperability-gateway-service
```
