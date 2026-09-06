package com.nerdc.elephantfence.backend.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemHealthSnapshotDTO {
    private String overallState; // HEALTHY, DEGRADED, UNHEALTHY
    private Long uptimeSeconds;
    private Integer activeIssues;
    private OffsetDateTime checkedAt;
    private List<ServiceHealthDTO> services;
    private GatewaySummaryHealthDTO gatewaySummary;
    private List<UnhealthyGatewayHealthDTO> unhealthyGateways;
    private List<BackgroundJobHealthDTO> jobs;
    private List<SystemHealthEventDTO> events;
}
