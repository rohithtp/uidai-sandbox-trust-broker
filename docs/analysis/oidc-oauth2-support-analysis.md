# OIDC / OAuth2 Support Analysis

## Executive Summary

The UIDAI Sandbox Trust Broker **does not currently implement OAuth2 or OIDC**. It acts as an **OAuth2 Resource Server** (JWT consumer/validator) and a **custom token translation layer**, but it does not expose any OAuth2 authorization server endpoints, OIDC discovery, or standard grant-type flows.

This document covers:
1. What is currently in place
2. What is missing relative to full OAuth2/OIDC conformance
3. What changes would be required to add support

---

## 1. Current State

### Architecture Overview

The system consists of two Spring Boot 3.2 services:

| Service | Port | Role |
|---|---|---|
| `interoperability-gateway-service` | 8081 | System registration, trust validation, async routing via Kafka |
| `token-verification-and-translation-service` | 8082 | JWT verification, token re-issuance (sandbox session tokens) |

### What Is Already OAuth2-Adjacent

#### `spring-boot-starter-oauth2-resource-server` (port 8082)

`token-verification-and-translation-service/pom.xml` includes:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

`token/config/SecurityConfig.java` configures Spring Security as an OAuth2 resource server:

```java
.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())))
```

This means the service can **validate incoming JWTs** signed by an external Identity Provider (IdP) whose JWKS URI is configured at `app.security.jwks-uri`. The JWKS response is cached in Redis for 1 hour.

Only **RS256** is accepted (`JWSAlgorithm.RS256` is explicitly enforced in `SecurityConfig.java`).

#### Token Translation (Custom, Not OAuth2)

After validating an incoming JWT, `TokenServiceImpl` issues a **new signed JWT** (called a "sandbox session token") using a `NimbusJwtEncoder` backed by an in-memory RSA keypair generated at startup.

Claims in the issued token:

| Claim | Value |
|---|---|
| `iss` | `"uidai-trust-broker"` (hardcoded) |
| `sub` | Inherited from incoming JWT subject |
| `iat` | Current timestamp |
| `exp` | `iat + 30 minutes` |
| `originSystem` | `systemId` from the request |
| `trustLevel` | `"HIGH"` (hardcoded) |
| `tokenType` | `"SANDBOX_SESSION_TOKEN"` |
| `normalizedName` | Uppercased `name` claim from incoming JWT |

This is a **custom proprietary flow**, not an OAuth2 grant type.

#### JWKS Consumption (Not Publication)

`JwksService.java` fetches the JWKS document from an external IdP URI (`app.security.jwks-uri`, defaulting to the placeholder `https://example.com/.well-known/jwks.json`) and caches the key set in Redis. The service **consumes** JWKS from an external party but **does not publish** its own JWKS endpoint.

### What Is Explicitly Missing

The following are **absent from the codebase**:

| OAuth2 / OIDC Feature | Status |
|---|---|
| `/.well-known/openid-configuration` (Discovery) | **Not present** |
| `/.well-known/jwks.json` (JWKS publication) | **Not present** |
| `/oauth2/authorize` (Authorization endpoint) | **Not present** |
| `/oauth2/token` (Token endpoint) | **Not present** |
| `/oauth2/userinfo` (UserInfo endpoint) | **Not present** |
| `/oauth2/introspect` (Token introspection) | **Not present** |
| `/oauth2/revoke` (Token revocation) | **Not present** |
| Grant types (authorization_code, client_credentials, etc.) | **Not present** |
| Scope handling | **Not present** |
| Client registration (`ClientRegistration`) | **Not present** |
| `spring-authorization-server` dependency | **Not present** |
| Persistent signing keys (durable across restarts) | **Not present** — keypair is ephemeral, regenerated on every boot |
| PKCE support | **Not present** |
| Refresh token flow | **Not present** |

The gateway service (`port 8081`) uses plain `spring-boot-starter-security` with **all endpoints open** (`anyRequest().permitAll()`), with a comment noting that mTLS/API key/OAuth2 client credentials are production-only concerns.

---

## 2. Gap Analysis

### 2.1 No Authorization Server

The system relies entirely on an **external IdP** to authenticate users and issue the initial JWT. There is no mechanism for a client to obtain a token directly from this system via a standard grant type.

### 2.2 No OIDC Identity Layer

There is no concept of a session, user consent, or identity claims beyond what is forwarded from the incoming JWT. OIDC requires:
- A `sub` claim (present, inherited)
- A UserInfo endpoint (absent)
- ID tokens with `nonce`, `auth_time`, `acr` (absent)
- Discovery document (absent)

### 2.3 Ephemeral Signing Keys

Translated tokens are signed with a keypair generated fresh on every service restart. Any relying party that cached the public key (or the translated token itself) will see signature validation failures after a restart. This violates basic key management requirements for any production token flow.

### 2.4 No Client Registry

There is no way for a client application to register itself, obtain a `client_id`/`client_secret`, or be granted specific scopes. The `SystemRegistry` in Redis tracks participating systems (for routing purposes), but it is not an OAuth2 client registry.

### 2.5 Gateway Has No Authentication

The interoperability gateway (port 8081) allows all requests without any form of authentication. In an OAuth2 model the gateway would enforce bearer token validation before routing.

---

## 3. Required Changes to Add OAuth2 / OIDC Support

The required work falls into three tiers depending on the desired conformance level.

---

### Tier 1 — Minimal: Expose JWKS and Discovery Endpoints

**Goal:** Allow external relying parties to validate the sandbox session tokens issued by this service.

**Changes required:**

#### 3.1.1 Persist the RSA Signing Keypair

Currently `SecurityConfig.generateRsaKey()` creates a new in-memory keypair on every boot. This must be replaced with a persisted key.

Options:
- Store the RSA private key in a secrets manager (AWS Secrets Manager, HashiCorp Vault) and load at startup
- Store PEM-encoded keys in `application.properties` / Kubernetes secrets (acceptable for sandbox)

```java
// Replace:
KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

// With: load from config
@Value("${app.security.rsa-private-key}")
RSAPrivateKey privateKey;

@Value("${app.security.rsa-public-key}")
RSAPublicKey publicKey;
```

#### 3.1.2 Add a JWKS Endpoint

Add a controller to `token-verification-and-translation-service` that exposes the public key in JWK Set format:

```java
@RestController
public class JwksController {

    private final JWKSet jwkSet;  // built from the persistent public key

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return jwkSet.toJSONObject();
    }
}
```

Permit this endpoint in `SecurityConfig` (`anyRequest` rules).

#### 3.1.3 Add a Minimal OIDC Discovery Document

```java
@GetMapping("/.well-known/openid-configuration")
public Map<String, Object> discovery() {
    return Map.of(
        "issuer", "https://<trust-broker-host>",
        "jwks_uri", "https://<trust-broker-host>/.well-known/jwks.json",
        "token_endpoint", "https://<trust-broker-host>/oauth2/token",
        "response_types_supported", List.of("code"),
        "subject_types_supported", List.of("public"),
        "id_token_signing_alg_values_supported", List.of("RS256")
    );
}
```

**Effort:** ~2 days. No new dependencies required.

---

### Tier 2 — Standard: Add OAuth2 Authorization Server

**Goal:** Allow clients to obtain tokens via standard OAuth2 grant types (e.g., `client_credentials` for machine-to-machine, `authorization_code` for user-facing flows).

**New dependency:**

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-authorization-server</artifactId>
    <version>1.2.x</version>  <!-- matches Spring Boot 3.2 -->
</dependency>
```

#### 3.2.1 Client Registry

Replace (or extend) the existing `SystemRegistry` in Redis with an OAuth2 `RegisteredClient` store:

```java
@Bean
public RegisteredClientRepository registeredClientRepository(RedisTemplate<String, Object> redis) {
    return new RedisRegisteredClientRepository(redis);
}
```

Each registered client gets a `client_id`, `client_secret` (BCrypt hashed), allowed grant types, and scopes.

#### 3.2.2 Authorization Server Configuration

```java
@Configuration
@Import(OAuth2AuthorizationServerConfiguration.class)
public class AuthorizationServerConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults());
        return http.build();
    }
}
```

This automatically registers:
- `POST /oauth2/token`
- `GET /oauth2/authorize`
- `GET /.well-known/openid-configuration`
- `GET /oauth2/jwks` (publishes the authorization server's own JWKS)
- `POST /oauth2/introspect`
- `POST /oauth2/revoke`

#### 3.2.3 Token Customizer

Integrate the existing trust-broker claims (`trustLevel`, `originSystem`, `tokenType`) into the OAuth2 token via a `OAuth2TokenCustomizer<JwtEncodingContext>`:

```java
@Bean
public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
    return context -> {
        if (context.getTokenType() == OAuth2TokenType.ACCESS_TOKEN) {
            RegisteredClient client = context.getRegisteredClient();
            String trustLevel = lookupTrustLevel(client.getClientId());
            context.getClaims()
                .claim("trustLevel", trustLevel)
                .claim("originSystem", client.getClientId())
                .claim("tokenType", "SANDBOX_SESSION_TOKEN");
        }
    };
}
```

#### 3.2.4 Protect the Gateway with Bearer Token Enforcement

Add `spring-boot-starter-oauth2-resource-server` to the gateway service and remove `anyRequest().permitAll()`:

```java
http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/registry/**").hasAuthority("SCOPE_registry:write")
        .requestMatchers("/api/v1/gateway/process").hasAuthority("SCOPE_gateway:process")
        .anyRequest().authenticated()
    )
    .oauth2ResourceServer(oauth2 -> oauth2
        .jwt(jwt -> jwt.jwkSetUri("https://<trust-broker-host>/oauth2/jwks"))
    );
```

**Effort:** ~1 week (new dependency, client registry, gateway auth enforcement, integration tests).

---

### Tier 3 — Full OIDC: Add Identity Federation (SAML → OIDC Bridge)

**Goal:** Accept SAML assertions from UIDAI's existing IdP and issue OIDC ID tokens, making the trust broker a full federation/translation layer.

This aligns with the `docs/saml-integration-plan.md` roadmap already in the repository.

**New dependency:**

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-saml2-service-provider</artifactId>
</dependency>
```

#### 3.3.1 SAML Service Provider Configuration

```java
@Bean
public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
    RelyingPartyRegistration registration = RelyingPartyRegistration
        .withRegistrationId("uidai-idp")
        .assertingPartyDetails(party -> party
            .entityId("https://idp.uidai.gov.in/saml2/idp/metadata.xml")
            .ssoServiceLocation("https://idp.uidai.gov.in/saml2/idp/SSO")
            .verificationX509Credentials(...)
        )
        .build();
    return new InMemoryRelyingPartyRegistrationRepository(registration);
}
```

#### 3.3.2 SAML → OIDC Claim Mapping

Implement a `Saml2AuthenticationTokenConverter` or custom `AuthenticationSuccessHandler` that maps SAML attributes (UID, name, aadhaar verification level) to standard OIDC claims:

| SAML Attribute | OIDC Claim |
|---|---|
| `UID` | `sub` |
| `name` | `name` |
| `verificationLevel` | `acr` (Authentication Context Class Reference) |
| `authTime` | `auth_time` |

#### 3.3.3 ID Token Issuance

The authorization server (from Tier 2) issues an OIDC ID token alongside the access token when the `openid` scope is requested. The ID token carries the mapped SAML claims.

**Effort:** ~3 weeks (SAML SP setup, claim mapping, end-to-end federation testing, key management for both SAML and OIDC).

---

## 4. Recommended Phased Approach

| Phase | Scope | Effort | Outcome |
|---|---|---|---|
| **Phase 1** | Persist signing key + expose `/.well-known/jwks.json` + discovery document | 2 days | Relying parties can validate sandbox session tokens. Interoperability unblocked. |
| **Phase 2** | Add Spring Authorization Server; `client_credentials` grant for M2M | 1 week | Standard OAuth2 token issuance. Gateway enforces bearer tokens. |
| **Phase 3** | `authorization_code` + PKCE for user-facing flows; OIDC UserInfo endpoint | 1 week | Full OAuth2/OIDC authorization server conformance. |
| **Phase 4** | SAML SP + SAML→OIDC claim bridge | 3 weeks | Full identity federation from UIDAI SAML IdP to OIDC-consuming relying parties. |

---

## 5. Key Configuration Properties Needed

The following properties are currently absent and would be needed:

```yaml
# application.yml (token-verification-and-translation-service)
spring:
  security:
    oauth2:
      authorizationserver:
        issuer: https://trust-broker.uidai.sandbox

app:
  security:
    # Replace ephemeral key generation with persistent keys
    rsa-private-key: classpath:keys/private.pem  # or from Vault
    rsa-public-key: classpath:keys/public.pem
    # External IdP JWKS URI (already exists, but needs a real value)
    jwks-uri: https://idp.uidai.gov.in/.well-known/jwks.json
```

---

## 6. Summary

| Capability | Current Status | Notes |
|---|---|---|
| Validate incoming JWTs from external IdP | **Supported** | RS256 only; JWKS fetched from configured URI |
| Issue custom signed JWTs | **Supported** | Ephemeral keys; not suitable for production |
| Expose JWKS for relying parties | **Not supported** | Phase 1 change |
| OIDC Discovery document | **Not supported** | Phase 1 change |
| OAuth2 Token endpoint | **Not supported** | Phase 2 change |
| OAuth2 `client_credentials` grant | **Not supported** | Phase 2 change |
| OAuth2 `authorization_code` + PKCE | **Not supported** | Phase 3 change |
| OIDC UserInfo endpoint | **Not supported** | Phase 3 change |
| SAML → OIDC federation | **Not supported** | Phase 4 change (planned in `docs/saml-integration-plan.md`) |
| Gateway bearer token enforcement | **Not supported** | Phase 2 change |
