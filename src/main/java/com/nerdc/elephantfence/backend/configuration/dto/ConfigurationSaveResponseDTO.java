package com.nerdc.elephantfence.backend.configuration.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationSaveResponseDTO {
    private String section;
    private Map<String, Object> value;
    private String updatedBy;
    private OffsetDateTime updatedAt;
    private Integer version;
}
