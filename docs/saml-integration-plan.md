# SAML Integration Plan for UIDAI Trust Broker

## Current State

The project has a JWT-only pipeline:

- **Gateway (8081)**: Routes requests, no auth enforcement
- **Token Service (8082)**: Validates JWTs via JWKS, re-signs as sandbox session tokens
- **No SAML**: No XML parsing, no assertion handling, no metadata endpoints

---

## Changes Required

### 1. New Maven Dependencies

**`trust-broker-common/pom.xml`** and/or **`token-verification-and-translation-service/pom.xml`**:

```xml
<!-- Spring Security SAML2 (Spring Boot 3.x compatible) -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-saml2-service-provider</artifactId>
</dependency>

<!-- OpenSAML (if lower-level assertion parsing is needed) -->
<dependency>
    <groupId>org.opensaml</groupId>
    <artifactId>opensaml-saml-impl</artifactId>
    <version>4.x</version>
</dependency>
```

---

### 2. New/Modified DTOs (`trust-broker-common`)

**Modify `TokenRequest.java`** — add a `tokenFormat` discriminator:

```java
public enum TokenFormat { JWT, SAML }

String tokenFormat;     // "JWT" or "SAML"
String samlAssertion;   // Base64-encoded SAML assertion (SAML flow)
String token;           // existing field (JWT flow)
```

**New `SamlAssertionDetails.java`**:

```java
String nameId;
String nameIdFormat;    // urn:oasis:names:tc:SAML:2.0:nameid-format:...
String issuer;
Instant notBefore;
Instant notOnOrAfter;
Map<String, List<String>> attributes;   // from AttributeStatement
String sessionIndex;
```

**Modify `TokenResponse.java`** — add:

```java
SamlAssertionDetails samlDetails;   // populated when input was SAML
```

---

### 3. New Service: `SamlAssertionService` (Token Verification Service)

Mirrors the existing `JwksService` pattern. Responsibilities:

- Fetch and cache IdP **SAML metadata XML** from the configured URI (Redis, same 1-hour TTL pattern as JWKS)
- Parse `IDPSSODescriptor` to extract signing certificates
- Validate SAML assertion signature against the IdP certificate
- Validate `Conditions` (NotBefore, NotOnOrAfter, AudienceRestriction)
- Extract `NameID` and `AttributeStatement` into `SamlAssertionDetails`

New configuration properties in `application.properties`:

```properties
app.security.saml.idp-metadata-uri=https://<idp>/saml/metadata
app.security.saml.sp-entity-id=https://uidai-trust-broker/saml/sp
app.security.saml.expected-audience=https://uidai-trust-broker
```

---

### 4. Modify `TokenServiceImpl` (Token Verification Service)

Currently handles only JWT. Add a branching path based on `TokenRequest.tokenFormat`:

```
verifyAndTranslate(TokenRequest)
  ├── if JWT  → existing JwtDecoder + JwksService path (unchanged)
  └── if SAML → new SamlAssertionService path
                  → validate assertion signature
                  → check Conditions (NotBefore, NotOnOrAfter, Audience)
                  → extract NameID + attributes
                  → map to normalized claims
                  → sign new sandbox session JWT (same step as JWT path)
```

The re-signing step (creating a new JWT with `normalizedName`) is reusable — SAML flows produce a JWT output, just from a SAML input.

---

### 5. New `SamlMetadataCacheConfig` (Token Verification Service)

Mirror of `RedisCacheConfig.java`. Add a cache entry for SAML IdP metadata:

```java
RedisCacheConfiguration.defaultCacheConfig()
    .entryTtl(Duration.ofHours(1))
    .serializeValuesWith(...)
```

Cache name: `"samlMetadata"`

---

### 6. Modify `SecurityConfig` (Token Verification Service)

Current config: OAuth2 resource server with JWT decoder.

**Option A — Recommended for this architecture**: Keep the existing JWT resource server. Add `/api/v1/token/verify` as `permitAll` with SAML validation handled in service logic, not the Spring Security filter chain. Lowest disruption, correct for machine-to-machine trust broker flows.

**Option B**: Add `saml2Login()` to Spring Security if the service needs to act as a SAML SP for browser-based SSO redirects (HTTP-Redirect / POST bindings). Only needed for interactive browser SSO — not applicable to the current M2M model.

**Recommendation**: Use Option A.

---

### 7. New/Modified Controller Endpoint (Token Verification Service)

Extend `TokenController.java` to accept SAML input via the unified endpoint:

```
POST /api/v1/token/verify
Content-Type: application/json

{
  "tokenFormat": "SAML",
  "samlAssertion": "<Base64-encoded signed SAML assertion>",
  "systemId": "system-001"
}
```

The unified endpoint approach is preferred over a separate `/verify-saml` endpoint since existing routing rules already dispatch to `/api/v1/token/verify`.

---

### 8. Modify `ExternalSystem` / Registry (Gateway Service)

Add SAML-aware fields to system registration in `ExternalSystem.java`:

```java
String identityProtocol;    // "JWT" or "SAML"
String idpEntityId;         // for SAML-registered systems
String idpMetadataUri;      // per-system IdP metadata override (optional)
```

The `RoutingRule` already has a `protocol` field — verify it is populated correctly to route SAML vs JWT requests to the right verification path.

---

### 9. New Tests Required

| Test Class | What to Test |
|---|---|
| `SamlAssertionServiceTest` | Signature validation, expired assertions, bad audience, attribute extraction |
| `TokenServiceImplSamlTest` | Full SAML → JWT translation flow with mocked `SamlAssertionService` |
| `TokenControllerSamlTest` | POST `/api/v1/token/verify` with `tokenFormat=SAML` input |
| `SamlMetadataCacheTest` | Redis cache hit/miss for IdP metadata |

Use signed test SAML assertions generated with a test RSA key. Do not mock the signature validation step itself — test it against a real test certificate.

---

### 10. E2E Test Script (`e2e_test.sh`)

Add a SAML scenario:

1. Register a system with `identityProtocol=SAML`
2. POST a Base64-encoded signed test SAML assertion to `/api/v1/token/verify`
3. Assert the translated JWT contains correct `sub`, `normalizedName`, and `saml_issuer` claims

---

## What Stays the Same

| Component | Reused As-Is |
|---|---|
| Redis caching infrastructure | Cache SAML metadata (same pattern as JWKS) |
| Kafka audit events | Emit SAML verification events (same `AuditEvent` DTO) |
| JWT re-signing / sandbox token | Output is always a JWT regardless of input format |
| System Registry & Trust Levels | Extended with `identityProtocol` field |
| Gateway routing | Unchanged — routes to token service |

## What's New

| Component | New Work |
|---|---|
| `SamlAssertionService` | Core new class — SAML equivalent of `JwksService` |
| `SamlAssertionDetails` DTO | New DTO for extracted SAML claims |
| `TokenRequest` format discriminator | Minor DTO change |
| IdP metadata fetching + caching | New config + Redis cache entry |
| SAML signature validation logic | Via `spring-security-saml2-service-provider` |
| SAML-specific unit + integration tests | New test classes |
| `application.properties` SAML config | 3–4 new properties |

---

## Implementation Risk

The biggest risk is **SAML signature validation** — XML DSig with certificate chain validation is complex. Using `spring-security-saml2-service-provider` rather than raw OpenSAML reduces this significantly since Spring handles the XML crypto plumbing.
