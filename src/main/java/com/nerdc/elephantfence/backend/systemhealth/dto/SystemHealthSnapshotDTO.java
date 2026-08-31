package com.nerdc.elephantfence.backend.systemhealth.dto;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemHealthSnapshotDTO {
    private String overallState;
    private long uptimeSeconds;
    private int activeIssues;
    private OffsetDateTime checkedAt;
    private List<CoreServiceHealthDTO> services;
    private GatewayHealthSummaryDTO gatewaySummary;
    private List<UnhealthyGatewayDTO> unhealthyGateways;
    private List<BackgroundJobHealthDTO> jobs;
    private List<SystemHealthEventDTO> events;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CoreServiceHealthDTO {
        private String id;
        private String name;
        private String state;
        private Long responseTimeMs;
        private OffsetDateTime lastSuccessfulCheck;
        private String message;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GatewayHealthSummaryDTO {
        private long total;
        private long online;
        private long offline;
        private long lateReporting;
        private double communicationSuccessPercent;
        private OffsetDateTime latestTelemetryAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnhealthyGatewayDTO {
        private Long id;
        private String code;
        private String fenceCode;
        private String state;
        private OffsetDateTime lastCommunicationAt;
        private OffsetDateTime nextExpectedAt;
        private long delayMinutes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BackgroundJobHealthDTO {
        private String id;
        private String name;
        private String result;
        private OffsetDateTime lastRunAt;
        private OffsetDateTime nextRunAt;
        private Long durationMs;
        private boolean retryAllowed;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SystemHealthEventDTO {
        private String id;
        private OffsetDateTime occurredAt;
        private String component;
        private String severity;
        private String message;
        private String status;
    }
}
