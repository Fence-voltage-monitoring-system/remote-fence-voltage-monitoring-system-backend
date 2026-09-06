# Fence Module Test Script (PowerShell for Windows)
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "      FENCE MODULE TEST SUITE" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080"
$ErrorActionPreference = "Stop"

# Step 1: Login
Write-Host "[1/7] Authenticating as Admin..." -ForegroundColor Yellow
$loginPayload = @{
    email    = "admin@nerdc.lk"
    password = "Admin@123456"
} | ConvertTo-Json

try {
    $loginResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -ContentType "application/json" -Body $loginPayload
    $token = $loginResp.accessToken
    $headers = @{
        Authorization = "Bearer $token"
    }
    Write-Host "  Success! Logged in as: $($loginResp.user.fullName) ($($loginResp.user.role))" -ForegroundColor Green
} catch {
    Write-Host "  Authentication failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Step 2: List All Fences
Write-Host "[2/7] Fetching all existing fences..." -ForegroundColor Yellow
$fences = Invoke-RestMethod -Uri "$baseUrl/api/fences" -Method Get -Headers $headers
Write-Host "  Existing fence count: $($fences.Count)" -ForegroundColor Green
Write-Host ""

# Step 3: Create a Fence
Write-Host "[3/7] Creating a new test fence..." -ForegroundColor Yellow
$createPayload = @{
    code        = "F-TEST-PS-001"
    name        = "Western Colombo Test Fence"
    provinceId  = 3
    districtId  = 9
    lengthKm    = 14.5
    health      = "OFFLINE"
} | ConvertTo-Json

$createdFence = Invoke-RestMethod -Uri "$baseUrl/api/fences" -Method Post -Headers $headers -ContentType "application/json" -Body $createPayload
$fenceId = $createdFence.id
Write-Host "  Fence created successfully! ID: $fenceId, Code: $($createdFence.code)" -ForegroundColor Green
Write-Host ""

# Step 4: Get Fence by ID
Write-Host "[4/7] Retrieving fence by ID: $fenceId..." -ForegroundColor Yellow
$fetchedFence = Invoke-RestMethod -Uri "$baseUrl/api/fences/$fenceId" -Method Get -Headers $headers
Write-Host "  Retrieved Fence: $($fetchedFence.name), Length: $($fetchedFence.lengthKm) km, Health: $($fetchedFence.health)" -ForegroundColor Green
Write-Host ""

# Step 5: Update Fence
Write-Host "[5/7] Updating fence details (ID: $fenceId)..." -ForegroundColor Yellow
$updatePayload = @{
    code        = "F-TEST-PS-001"
    name        = "Updated Colombo Test Fence"
    provinceId  = 3
    districtId  = 9
    lengthKm    = 18.2
    health      = "HEALTHY"
} | ConvertTo-Json

$updatedFence = Invoke-RestMethod -Uri "$baseUrl/api/fences/$fenceId" -Method Put -Headers $headers -ContentType "application/json" -Body $updatePayload
Write-Host "  Updated Name: $($updatedFence.name), Health: $($updatedFence.health)" -ForegroundColor Green
Write-Host ""

# Step 6: Filter Fences by Province and District
Write-Host "[6/7] Filtering fences by Western province (id=3) and Colombo district (id=9)..." -ForegroundColor Yellow
$filteredFences = Invoke-RestMethod -Uri "$baseUrl/api/fences?provinceId=3&districtId=9" -Method Get -Headers $headers
Write-Host "  Found $($filteredFences.Count) fence(s) matching criteria." -ForegroundColor Green
Write-Host ""

# Step 7: Delete Test Fence
Write-Host "[7/7] Cleaning up test fence (ID: $fenceId)..." -ForegroundColor Yellow
Invoke-RestMethod -Uri "$baseUrl/api/fences/$fenceId" -Method Delete -Headers $headers
Write-Host "  Fence $fenceId deleted successfully!" -ForegroundColor Green
Write-Host ""

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  ALL FENCE TESTS PASSED SUCCESSFULLY! " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
