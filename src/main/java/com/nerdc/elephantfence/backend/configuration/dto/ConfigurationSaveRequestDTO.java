package com.nerdc.elephantfence.backend.configuration.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationSaveRequestDTO {

    @NotNull(message = "Configuration value is required")
    private JsonNode value;

    @NotBlank(message = "Reason for configuration change is required")
    private String reason;
}
