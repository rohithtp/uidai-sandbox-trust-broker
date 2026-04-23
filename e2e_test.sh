#!/usr/bin/env bash
set -euo pipefail

# Configuration
GATEWAY_URL="http://localhost:8081/api/v1"
TOKEN_URL="http://localhost:8082/api/v1"

echo "=== Starting E2E Test Flow ==="

# Helper: pretty-print JSON if available, else raw output
pretty() {
  if command -v python3 &>/dev/null; then
    python3 -m json.tool 2>/dev/null || cat
  else
    cat
  fi
}

# 1. System Registration
echo ""
echo "Step 1: Registering External System..."
curl -sf -X POST "$GATEWAY_URL/registry/systems" \
  -H "Content-Type: application/json" \
  -d '{
    "systemId": "UIDAI-SND-001",
    "systemName": "Sandbox Consumer 001",
    "trustLevel": "HIGH",
    "active": true
  }' | pretty

# 2. Add Routing Rule
echo ""
echo "Step 2: Adding Routing Rule..."
curl -sf -X POST "$GATEWAY_URL/registry/rules" \
  -H "Content-Type: application/json" \
  -d '{
    "ruleId": "RULE-001",
    "systemId": "UIDAI-SND-001",
    "priority": 1,
    "targetTopic": "token-verification-topic",
    "protocol": "KAFKA",
    "active": true
  }' | pretty

# 3. Token Dispatch (via Gateway)
echo ""
echo "Step 3: Dispatching Token Request via Gateway..."
# The Gateway looks up the routing rule for UIDAI-SND-001 and publishes the
# token to the configured Kafka topic. The response is an immediate ACK.
curl -sf -X POST "$GATEWAY_URL/gateway/process" \
  -H "Content-Type: application/json" \
  -d '{
    "systemId": "UIDAI-SND-001",
    "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoyNjA0MTEwMjAyfQ.dummy-signature"
  }' | pretty

echo ""
echo "=== E2E Test Flow Completed ==="
