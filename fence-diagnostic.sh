#!/bin/bash

echo "========================================="
echo "  FENCE DIAGNOSTIC TOOL"
echo "========================================="
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@nerdc.lk","password":"Admin@123456"}' \
  | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo -e "${RED}Failed to login${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Token obtained${NC}"
echo ""

# 1. Get provinces
echo -e "${YELLOW}1. Getting provinces...${NC}"
PROVINCES=$(curl -s -X GET http://localhost:8080/api/locations/provinces \
  -H "Authorization: Bearer $TOKEN")
echo "$PROVINCES" | python3 -m json.tool 2>/dev/null || echo "$PROVINCES"
echo ""

# 2. Get districts
echo -e "${YELLOW}2. Getting districts...${NC}"
DISTRICTS=$(curl -s -X GET http://localhost:8080/api/locations/districts \
  -H "Authorization: Bearer $TOKEN")
echo "$DISTRICTS" | python3 -m json.tool 2>/dev/null || echo "$DISTRICTS"
echo ""

# 3. Try creating a fence with full details
echo -e "${YELLOW}3. Creating fence with full details...${NC}"
RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST http://localhost:8080/api/fences \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "F-TEST-001",
    "name": "Test Colombo Fence",
    "provinceId": 1,
    "districtId": 10,
    "lengthKm": 12.5,
    "health": "OFFLINE"
  }')
echo "$RESPONSE"
echo ""

# 4. Try creating without health
echo -e "${YELLOW}4. Creating fence without health status...${NC}"
RESPONSE2=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST http://localhost:8080/api/fences \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "F-TEST-002",
    "name": "Test Fence 2",
    "provinceId": 1,
    "districtId": 10,
    "lengthKm": 15.0
  }')
echo "$RESPONSE2"
echo ""

# 5. Try creating with different province
echo -e "${YELLOW}5. Creating fence with province 1...${NC}"
RESPONSE3=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST http://localhost:8080/api/fences \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "F-TEST-003",
    "name": "Test Fence 3",
    "provinceId": 1,
    "districtId": 1
  }')
echo "$RESPONSE3"
echo ""

echo -e "${GREEN}=========================================${NC}"
