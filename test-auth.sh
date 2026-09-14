#!/bin/bash

echo "========================================="
echo "  AUTHENTICATION TESTING"
echo "========================================="
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Test 1: Check if we can register
echo -e "${YELLOW}1. Testing registration...${NC}"
REGISTER_ENDPOINTS=(
    "/api/auth/register"
    "/api/register"
    "/auth/register"
    "/register"
)

for endpoint in "${REGISTER_ENDPOINTS[@]}"; do
    RESPONSE=$(curl -s -X POST "http://localhost:8080$endpoint" \
        -H "Content-Type: application/json" \
        -d '{"username":"testadmin","password":"admin123","email":"admin@test.com","role":"SUPER_ADMIN"}' 2>/dev/null)
    
    if [ -n "$RESPONSE" ] && [ "$RESPONSE" != "{}" ] && [ "$RESPONSE" != "null" ]; then
        echo -e "${GREEN}✓ Registration works at: $endpoint${NC}"
        echo "Response: $RESPONSE"
        break
    fi
done
echo ""

# Test 2: Try to login with common credentials
echo -e "${YELLOW}2. Testing login with common credentials...${NC}"
LOGIN_ENDPOINTS=(
    "/api/auth/login"
    "/api/login"
    "/auth/login"
    "/login"
)

CREDENTIALS=(
    '{"username":"admin","password":"admin"}'
    '{"username":"admin","password":"admin123"}'
    '{"username":"testadmin","password":"admin123"}'
    '{"username":"user","password":"password"}'
    '{"username":"superadmin","password":"superadmin"}'
)

for endpoint in "${LOGIN_ENDPOINTS[@]}"; do
    for cred in "${CREDENTIALS[@]}"; do
        RESPONSE=$(curl -s -X POST "http://localhost:8080$endpoint" \
            -H "Content-Type: application/json" \
            -d "$cred" 2>/dev/null)
        
        TOKEN=$(echo "$RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
        if [ -n "$TOKEN" ]; then
            echo -e "${GREEN}✓ Login successful!${NC}"
            echo "  Endpoint: $endpoint"
            echo "  Credentials: $cred"
            echo "  Token: ${TOKEN:0:50}..."
            echo ""
            echo "export TOKEN=\"$TOKEN\"" > token.env
            echo -e "${GREEN}Token saved to token.env${NC}"
            
            # Test fence endpoints with this token
            echo -e "${YELLOW}Testing fence endpoints with token...${NC}"
            FENCE_RESPONSE=$(curl -s -X GET http://localhost:8080/api/fences \
                -H "Authorization: Bearer $TOKEN" 2>/dev/null)
            HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/fences \
                -H "Authorization: Bearer $TOKEN" 2>/dev/null)
            
            if [ "$HTTP_CODE" = "200" ]; then
                echo -e "${GREEN}✓ Successfully accessed fences with token!${NC}"
                echo "Response: $FENCE_RESPONSE" | head -50
                exit 0
            else
                echo -e "${RED}✗ Failed to access fences with token (HTTP $HTTP_CODE)${NC}"
            fi
            exit 0
        fi
    done
done

echo -e "${RED}✗ No login combination worked.${NC}"
