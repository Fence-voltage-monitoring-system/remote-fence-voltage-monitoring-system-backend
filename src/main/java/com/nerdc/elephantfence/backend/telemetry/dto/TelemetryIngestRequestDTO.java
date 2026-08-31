package com.nerdc.elephantfence.backend.telemetry.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelemetryIngestRequestDTO {

    @NotBlank(message = "Device serial number is required")
    @Size(max = 100, message = "Serial number must not exceed 100 characters")
    private String deviceSerial;

    @NotNull(message = "Voltage reading is required")
    private BigDecimal voltage; // measured voltage in kV (e.g. 5.6)

    @Min(value = 0, message = "Battery level cannot be less than 0%")
    @Max(value = 100, message = "Battery level cannot exceed 100%")
    private Integer battery; // optional battery percentage (0-100)

    @Min(value = 0, message = "Signal strength cannot be less than 0%")
    @Max(value = 100, message = "Signal strength cannot exceed 100%")
    private Integer signal; // optional signal strength (0-100)
}
