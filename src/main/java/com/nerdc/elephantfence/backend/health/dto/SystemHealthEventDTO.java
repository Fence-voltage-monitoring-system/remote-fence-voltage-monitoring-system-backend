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
public class SystemHealthEventDTO {
    private String id;
    private OffsetDateTime occurredAt;
    private String component;
    private String severity; // INFO, WARNING, CRITICAL
    private String message;
    private String status; // OPEN, RESOLVED
}
