# Trust Broker Common
 
 Part of the [UIDAI Sandbox Trust Broker](../README.md).
 
 This module provides shared components and libraries used by all services in the Trust Broker ecosystem.
 
 ---
 
 ## Responsibilities
 
 - **Centralized DTOs**: Standardized request and response objects (`TokenRequest`, `TokenResponse`, `ExternalSystem`, `RoutingRule`).
 - **Kafka Configuration**: Shared topic definitions and producer/consumer configuration templates.
 - **Error Handling**: Global exception handlers and standardized error response formats (`ErrorResponse`).
 - **Utility Classes**: JSON parsing utilities and shared constants.
 
 ---
 
 ## Key Components
 
 ### 1. Data Transfer Objects (DTOs)
 - `com.uidai.sandbox.common.dto.TokenRequest`: Unified structure for token processing requests.
 - `com.uidai.sandbox.common.dto.TokenResponse`: Standardized response for verification results.
 - `com.uidai.sandbox.common.dto.ExternalSystem`: Metadata for system registry.
 - `com.uidai.sandbox.common.dto.RoutingRule`: Definition for dynamic Kafka routing.
 
 ### 2. Messaging
 - `com.uidai.sandbox.common.config.KafkaTopicConfig`: Centralized topic management.
 
 ---
 
 ## Usage
 
 To use this module in a new service, add it as a dependency in your `pom.xml`:
 
 ```xml
 <dependency>
     <groupId>com.uidai.sandbox</groupId>
     <artifactId>trust-broker-common</artifactId>
     <version>${project.version}</version>
 </dependency>
 ```
 
 ---
 
 ## Dependencies
 
 | Dependency | Purpose |
 |---|---|
 | `spring-boot-starter` | Core Spring Boot support |
 | `spring-kafka` | Infrastructure for Kafka messaging |
 | `lombok` | Boilerplate reduction for DTOs |
 | `jackson-databind` | JSON serialization |
