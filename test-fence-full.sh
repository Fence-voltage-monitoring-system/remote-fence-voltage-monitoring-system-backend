#!/bin/bash

echo "========================================="
echo "  FENCE MODULE COMPLETE TEST"
echo "========================================="
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Step 1: Login
echo -e "${YELLOW}Step 1: Logging in...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@nerdc.lk","password":"Admin@123456"}')

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo -e "${RED}✗ Login failed!${NC}"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi

echo -e "${GREEN}✓ Login successful!${NC}"
echo "Token: ${TOKEN:0:50}..."
echo ""

# Step 2: List all fences
echo -e "${YELLOW}Step 2: Listing all fences...${NC}"
RESPONSE=$(curl -s -X GET http://localhost:8080/api/fences \
  -H "Authorization: Bearer $TOKEN")
echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
echo ""

# Step 3: Create a fence
echo -e "${YELLOW}Step 3: Creating a new fence...${NC}"
CREATE_RESPONSE=$(curl -s -X POST http://localhost:8080/api/fences \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "F-TEST-001",
    "name": "Test Colombo Fence",
    "provinceId": 3,
    "districtId": 9,
    "lengthKm": 12.5,
    "health": "OFFLINE"
  }')

echo "$CREATE_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$CREATE_RESPONSE"

# Extract fence ID
FENCE_ID=$(echo "$CREATE_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

if [ -n "$FENCE_ID" ]; then
    echo -e "${GREEN}✓ Fence created with ID: $FENCE_ID${NC}"
else
    echo -e "${RED}✗ Failed to create fence${NC}"
    FENCE_ID=1
fi
echo ""

# Step 4: Get fence by ID
if [ -n "$FENCE_ID" ]; then
    echo -e "${YELLOW}Step 4: Getting fence with ID: $FENCE_ID${NC}"
    RESPONSE=$(curl -s -X GET http://localhost:8080/api/fences/$FENCE_ID \
      -H "Authorization: Bearer $TOKEN")
    echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
    echo ""
fi

# Step 5: Update fence
if [ -n "$FENCE_ID" ] && [ "$FENCE_ID" != "null" ]; then
    echo -e "${YELLOW}Step 5: Updating fence with ID: $FENCE_ID${NC}"
    UPDATE_RESPONSE=$(curl -s -X PUT http://localhost:8080/api/fences/$FENCE_ID \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "code": "F-TEST-001-UPDATED",
        "name": "Updated Colombo Fence",
        "provinceId": 3,
        "districtId": 9,
        "lengthKm": 15.0,
        "health": "HEALTHY"
      }')
    echo "$UPDATE_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$UPDATE_RESPONSE"
    echo ""
fi

# Step 6: Filter fences by location
echo -e "${YELLOW}Step 6: Filtering fences by location (provinceId=3, districtId=9)${NC}"
RESPONSE=$(curl -s -X GET "http://localhost:8080/api/fences?provinceId=3&districtId=9" \
  -H "Authorization: Bearer $TOKEN")
echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
echo ""

# Step 7: Test different health statuses
echo -e "${YELLOW}Step 7: Testing different health statuses${NC}"
for status in "HEALTHY" "WARNING" "CRITICAL" "OFFLINE"; do
    echo -e "${BLUE}Creating fence with status: $status${NC}"
    RESPONSE=$(curl -s -X POST http://localhost:8080/api/fences \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "{
        \"code\": \"F-TEST-$status\",
        \"name\": \"Test $status Fence\",
        \"provinceId\": 3,
        \"districtId\": 9,
        \"lengthKm\": 10.0,
        \"health\": \"$status\"
      }")
    echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
    echo ""
done

echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}FENCE MODULE TESTING COMPLETE!${NC}"
