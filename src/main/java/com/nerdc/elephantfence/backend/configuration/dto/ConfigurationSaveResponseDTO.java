package com.nerdc.elephantfence.backend.configuration.dto;

import com.fasterxml.jackson.databind.JsonNode;
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
    private JsonNode value;
    private String updatedBy;
    private OffsetDateTime updatedAt;
    private Integer version;
}
