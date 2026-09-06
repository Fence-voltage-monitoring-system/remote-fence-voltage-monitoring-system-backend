package com.nerdc.elephantfence.backend.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceHealthDTO {
    private String id;
    private String name;
    private String state; // HEALTHY, DEGRADED, UNHEALTHY
    private Long responseTimeMs;
    private OffsetDateTime lastSuccessfulCheck;
    private String message;
}
