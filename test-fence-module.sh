#!/bin/bash
echo "========================================="
echo "  FENCE MODULE TESTING"
echo "========================================="
echo ""
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'
echo -e "${YELLOW}Step 1: Checking if authentication is required...${NC}"
TEST_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/fences)
if [ "$TEST_RESPONSE" = "200" ]; then
    echo -e "${GREEN}✓ No authentication required!${NC}"
    TOKEN=""
elif [ "$TEST_RESPONSE" = "401" ] || [ "$TEST_RESPONSE" = "403" ]; then
    echo -e "${YELLOW}⚠ Authentication required. Trying to login...${NC}"
    LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email":"admin@nerdc.lk","password":"Admin@123456"}' 2>/dev/null)
    TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    if [ -z "$TOKEN" ]; then
        LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"Admin@123456"}' 2>/dev/null)
        TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    fi
    if [ -z "$TOKEN" ]; then
        echo -e "${RED}✗ Could not get token. Testing without auth anyway...${NC}"
        TOKEN=""
    else
        echo -e "${GREEN}✓ Token obtained successfully!${NC}"
        echo "Token: ${TOKEN:0:50}..."
    fi
fi
echo ""
echo -e "${YELLOW}Step 2: Testing Fence CRUD Operations${NC}"
echo ""
echo -e "${BLUE}2.1 List All Fences${NC}"
if [ -n "$TOKEN" ]; then
    RESPONSE=$(curl -s -X GET http://localhost:8080/api/fences -H "Authorization: Bearer $TOKEN")
else
    RESPONSE=$(curl -s -X GET http://localhost:8080/api/fences)
fi
echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
echo ""
echo -e "${BLUE}2.2 Create a New Fence${NC}"
FENCE_DATA='{"code":"F-TEST-001","name":"Test Colombo Fence","provinceId":3,"districtId":9,"lengthKm":12.5,"health":"OFFLINE"}'
if [ -n "$TOKEN" ]; then
    CREATE_RESPONSE=$(curl -s -X POST http://localhost:8080/api/fences -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$FENCE_DATA")
else
    CREATE_RESPONSE=$(curl -s -X POST http://localhost:8080/api/fences -H "Content-Type: application/json" -d "$FENCE_DATA")
fi
echo "$CREATE_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$CREATE_RESPONSE"
FENCE_ID=$(echo "$CREATE_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
if [ -n "$FENCE_ID" ]; then
    echo -e "${GREEN}✓ Fence created with ID: $FENCE_ID${NC}"
else
    echo -e "${YELLOW}⚠ Could not extract ID from response${NC}"
    FENCE_ID=1
fi
echo ""
if [ -n "$FENCE_ID" ]; then
    echo -e "${BLUE}2.3 Get Fence by ID: $FENCE_ID${NC}"
    if [ -n "$TOKEN" ]; then
        RESPONSE=$(curl -s -X GET http://localhost:8080/api/fences/$FENCE_ID -H "Authorization: Bearer $TOKEN")
    else
        RESPONSE=$(curl -s -X GET http://localhost:8080/api/fences/$FENCE_ID)
    fi
    echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
    echo ""
fi
echo -e "${BLUE}2.4 Filter Fences by Location (provinceId=3, districtId=9)${NC}"
if [ -n "$TOKEN" ]; then
    RESPONSE=$(curl -s -X GET "http://localhost:8080/api/fences?provinceId=3&districtId=9" -H "Authorization: Bearer $TOKEN")
else
    RESPONSE=$(curl -s -X GET "http://localhost:8080/api/fences?provinceId=3&districtId=9")
fi
echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
echo ""
if [ -n "$FENCE_ID" ] && [ "$FENCE_ID" != "null" ]; then
    echo -e "${BLUE}2.5 Update Fence ID: $FENCE_ID${NC}"
    UPDATE_DATA='{"code":"F-TEST-001-UPDATED","name":"Updated Colombo Fence","provinceId":3,"districtId":9,"lengthKm":15.0,"health":"HEALTHY"}'
    if [ -n "$TOKEN" ]; then
        RESPONSE=$(curl -s -X PUT http://localhost:8080/api/fences/$FENCE_ID -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$UPDATE_DATA")
    else
        RESPONSE=$(curl -s -X PUT http://localhost:8080/api/fences/$FENCE_ID -H "Content-Type: application/json" -d "$UPDATE_DATA")
    fi
    echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
    echo ""
fi
echo -e "${BLUE}2.6 Delete Fence (skipping for safety)${NC}"
echo -e "${YELLOW}To delete, uncomment the DELETE section in the script${NC}"
echo ""
echo -e "${YELLOW}Step 3: Test Fence with Different Health Statuses${NC}"
echo ""
for status in "HEALTHY" "WARNING" "CRITICAL" "OFFLINE"; do
    echo -e "${BLUE}Creating fence with status: $status${NC}"
    FENCE_DATA="{\"code\":\"F-TEST-$status\",\"name\":\"Test $status Fence\",\"provinceId\":3,\"districtId\":9,\"lengthKm\":10.0,\"health\":\"$status\"}"
    if [ -n "$TOKEN" ]; then
        RESPONSE=$(curl -s -X POST http://localhost:8080/api/fences -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$FENCE_DATA")
    else
        RESPONSE=$(curl -s -X POST http://localhost:8080/api/fences -H "Content-Type: application/json" -d "$FENCE_DATA")
    fi
    echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
    echo ""
done
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}FENCE MODULE TESTING COMPLETE!${NC}"
