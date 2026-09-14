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
public class BackgroundJobHealthDTO {
    private String id;
    private String name;
    private String result; // SUCCESS, FAILED, RUNNING, SCHEDULED
    private OffsetDateTime lastRunAt;
    private OffsetDateTime nextRunAt;
    private Long durationMs;
    private boolean retryAllowed;
}
