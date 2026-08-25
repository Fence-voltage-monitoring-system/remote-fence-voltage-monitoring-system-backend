package com.nerdc.elephantfence.backend.gateways.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateGatewayRequestDTO {

    @Size(max = 100, message = "Gateway name must not exceed 100 characters")
    private String name;

    @Size(max = 100, message = "Serial number must not exceed 100 characters")
    private String serial;

    @Pattern(regexp = "^\\d{15}$", message = "IMEI must be exactly 15 digits")
    private String imei;

    @Size(max = 50, message = "Firmware version must not exceed 50 characters")
    private String firmware;

    private Boolean enabled;
}
