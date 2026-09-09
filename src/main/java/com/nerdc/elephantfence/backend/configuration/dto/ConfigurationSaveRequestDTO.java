package com.nerdc.elephantfence.backend.configuration.dto;

import java.util.Map;
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
    private Map<String, Object> value;

    @NotBlank(message = "Reason for configuration change is required")
    private String reason;
}
