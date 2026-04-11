#!/bin/bash

# Configuration
GATEWAY_URL="http://localhost:8081/api/v1"
TOKEN_URL="http://localhost:8082/api/v1"

echo "=== Starting E2E Test Flow ==="

# 1. System Registration
echo "Step 1: Registering External System..."
curl -s -X POST "$GATEWAY_URL/registry/systems" \
  -H "Content-Type: application/json" \
  -d '{
    "systemId": "UIDAI-SND-001",
    "systemName": "Sandbox Consumer 001",
    "trustLevel": "HIGH",
    "active": true
  }' | json_pp

# 2. Add Routing Rule
echo -e "\nStep 2: Adding Routing Rule..."
curl -s -X POST "$GATEWAY_URL/registry/rules" \
  -H "Content-Type: application/json" \
  -d '{
    "ruleId": "RULE-001",
    "systemId": "UIDAI-SND-001",
    "priority": 1,
    "targetTopic": "token-verification-topic",
    "protocol": "KAFKA",
    "active": true
  }' | json_pp

# 3. Token Dispatch (via Gateway)
echo -e "\nStep 3: Dispatching Token Request via Gateway..."
# Note: The Gateway internally sends to Kafka, but the current implementation might be returning the result if it's synchronous or just a placeholder.
# In a real async flow, the response might be 'ACCEPTED'.
curl -s -X POST "$GATEWAY_URL/gateway/process" \
  -H "Content-Type: application/json" \
  -d '{
    "systemId": "UIDAI-SND-001",
    "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoyNjA0MTEwMjAyfQ.dummy-signature"
  }' | json_pp

echo -e "\n=== E2E Test Flow Completed ==="
