package com.nerdc.elephantfence.backend.gateways.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayCreateRequestDTO {

    @NotBlank(message = "Gateway name is required")
    private String name;

    @NotBlank(message = "Serial number is required")
    private String serial;

    @NotBlank(message = "IMEI number is required")
    private String imei;

    private List<String> fences;

    private String firmware;
}
