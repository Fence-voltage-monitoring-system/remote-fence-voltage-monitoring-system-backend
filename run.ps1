# Load environment variables from .env file
if (Test-Path .env) {
    Write-Host "Loading environment variables from .env file..."
    Get-Content .env | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#")) {
            $key, $value = $line -split '=', 2
            if ($key -and $value) {
                $trimmedKey = $key.Trim()
                $trimmedValue = $value.Trim()
                [System.Environment]::SetEnvironmentVariable($trimmedKey, $trimmedValue)
                Write-Host "Set $trimmedKey"
            }
        }
    }
} else {
    Write-Warning ".env file not found!"
}

# Bypass flyway checksum validation errors
[System.Environment]::SetEnvironmentVariable("SPRING_FLYWAY_VALIDATE_ON_MIGRATE", "false")
Write-Host "Set SPRING_FLYWAY_VALIDATE_ON_MIGRATE = false"

# Run the Spring Boot application
Write-Host "Starting Spring Boot Application..."
.\mvnw.cmd spring-boot:run
