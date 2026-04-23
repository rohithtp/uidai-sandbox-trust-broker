# E2E Test Flow — `e2e_test.sh`

The script drives a three-step flow that exercises the full path from system registration through async token processing. It targets two services running locally:

| Service | Base URL |
|---|---|
| Interoperability Gateway | `http://localhost:8081/api/v1` |
| Token Verification & Translation | `http://localhost:8082/api/v1` |

---

## Main Flow (First Run / Happy Path)

### Step 1 — System Registration

**Request**
```
POST /api/v1/registry/systems
```
```json
{
  "systemId": "UIDAI-SND-001",
  "systemName": "Sandbox Consumer 001",
  "trustLevel": "HIGH",
  "active": true
}
```

**What happens**

`SystemRegistryController` → `SystemRegistryServiceImpl.registerSystem()`

1. Serializes the `ExternalSystem` DTO into Redis at key `system:UIDAI-SND-001`
2. Returns the registered object as-is

This is a plain Redis `SET` — re-running overwrites with the same data (idempotent).

---

### Step 2 — Add Routing Rule

**Request**
```
POST /api/v1/registry/rules
```
```json
{
  "ruleId": "RULE-001",
  "systemId": "UIDAI-SND-001",
  "priority": 1,
  "targetTopic": "token-verification-topic",
  "protocol": "KAFKA",
  "active": true
}
```

**What happens**

`SystemRegistryController` → `SystemRegistryServiceImpl.addRoutingRule()`

1. Stores the `RoutingRule` at Redis key `rule:RULE-001`
2. Adds `"RULE-001"` to the Redis Set `system_rules:UIDAI-SND-001`

Re-running is safe: Redis `SET` overwrites, `SADD` with a duplicate member is a no-op.

---

### Step 3 — Token Dispatch via Gateway

**Request**
```
POST /api/v1/gateway/process
```
```json
{
  "systemId": "UIDAI-SND-001",
  "token": "<JWT>"
}
```

**What happens**

`GatewayController` → `GatewayServiceImpl.processIncomingRequest()`

1. Fetches `system:UIDAI-SND-001` from Redis — found
2. Checks `active = true` — passes
3. Checks `trustLevel = HIGH` (not null, not LOW) — passes
4. Fetches routing rules from Redis Set `system_rules:UIDAI-SND-001` — finds `RULE-001`
5. Selects the highest-priority rule (priority=1, only one rule)
6. Protocol is `"KAFKA"` → calls `kafkaProducerService.sendToTopic(topic, request)`
7. `targetEndpoint` is `null` (see Step 2 note) → falls back to the hardcoded default: `"trust-broker.token.verification"`
8. Publishes `TokenRequest` to Kafka **asynchronously** (fire-and-forget)
9. Returns `ACCEPTED` immediately — the script ends here

**Response**
```json
{
  "status": "ACCEPTED",
  "message": "Request validated and routed for processing.",
  "details": {
    "gatewayId": "uidai-gateway-01",
    "receivedAt": "<timestamp>",
    "systemId": "UIDAI-SND-001",
    "trustLevel": "HIGH",
    "deliveryMode": "ASYNC_ROUTED"
  }
}
```

---

### Async Tail (after Step 3, not visible in script output)

`KafkaConsumerService` in the token-verification-service listens on `"trust-broker.token.verification"`:

1. Receives `TokenRequest`, calls `TokenServiceImpl.verifyAndTranslate()`
2. Decodes the JWT using `JwtDecoder` (JWKS-backed, signature key cached in Redis via `SecurityConfig`)
3. Validates signature and expiry
4. Extracts the `name` claim and normalizes it to uppercase
5. Issues a new `SANDBOX_SESSION_TOKEN` signed by the internal key, containing:
   - `originSystem`, `trustLevel=HIGH`, `tokenType=SANDBOX_SESSION_TOKEN`
   - `normalizedName` (uppercased)
   - 30-minute expiry
6. Returns `TokenResponse{status=VERIFIED, translatedToken=<new JWT>}` — this is consumed internally only; it is not returned to the original HTTP caller

---

## Subsequent Runs

The script is fully idempotent:

| Step | Why it is safe to re-run |
|---|---|
| Step 1 | Redis `SET` overwrites the existing key with the same data |
| Step 2 | `SET` overwrites the rule; `SADD` with a duplicate member is a no-op |
| Step 3 | Identical validation path; Kafka publish fires again |

---

## Alternative Flows

### Gateway rejects the request (synchronous, visible in script)

| Condition | Code location | Response `status` | Message |
|---|---|---|---|
| `systemId` not registered | `GatewayServiceImpl:35` | `REJECTED` | "System not registered" |
| System has `active = false` | `GatewayServiceImpl:42` | `REJECTED` | "System is inactive" |
| `trustLevel = LOW` or `null` | `GatewayServiceImpl:45` | `REJECTED` | "Insufficient trust level" |

### Gateway reroutes silently (request still accepted)

| Condition | Code location | Behaviour |
|---|---|---|
| No routing rules in Redis | `GatewayServiceImpl:53` | Logs warning; calls `sendTokenRequest()` → default topic `"trust-broker.token.verification"` |
| Rule protocol is not `"KAFKA"` | `GatewayServiceImpl:71` | Logs warning; falls back to default topic |
| Rule `targetEndpoint` is `null` | `GatewayServiceImpl:68` | Uses `KafkaTopicConfig.TOKEN_VERIFICATION_TOPIC` (`"trust-broker.token.verification"`) as fallback |

### Token verification failures (async, not visible in script)

| Condition | Code location | Response `status` |
|---|---|---|
| JWT signature invalid or token malformed | `TokenServiceImpl:80` | `FAILED` |
| JWT is expired | `TokenServiceImpl:80` (decoder throws) | `FAILED` |
| Kafka `send()` fails | `KafkaProducerServiceImpl:34` | Error logged only — gateway already returned `ACCEPTED` |
| Kafka consumer throws unexpectedly | `KafkaConsumerService:33` | Error logged; message is dropped (no Dead Letter Topic wired yet) |

---

## Flow Diagram

```
e2e_test.sh
│
├─ Step 1: POST /registry/systems
│           └─ Redis SET system:UIDAI-SND-001
│
├─ Step 2: POST /registry/rules
│           ├─ Redis SET rule:RULE-001
│           └─ Redis SADD system_rules:UIDAI-SND-001 "RULE-001"
│
└─ Step 3: POST /gateway/process
            ├─ Redis GET system:UIDAI-SND-001  ──► active? trustLevel sufficient?
            ├─ Redis SMEMBERS system_rules:UIDAI-SND-001  ──► pick highest-priority rule
            ├─ Kafka PRODUCE → "trust-broker.token.verification"
            └─ HTTP 200 ACCEPTED  (synchronous response ends here)

                          [async]
                          Kafka CONSUME "trust-broker.token.verification"
                          └─ TokenServiceImpl.verifyAndTranslate()
                              ├─ JwtDecoder.decode()  (JWKS via Redis cache)
                              ├─ Normalize name claim → uppercase
                              ├─ JwtEncoder.encode()  (new SANDBOX_SESSION_TOKEN)
                              └─ TokenResponse{status=VERIFIED}
```
