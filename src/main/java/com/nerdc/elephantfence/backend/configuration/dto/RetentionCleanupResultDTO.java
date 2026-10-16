package com.nerdc.elephantfence.backend.configuration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetentionCleanupResultDTO {
    private boolean success;
    private boolean manual;
    private int totalRecordsDeleted;
    private int deletedTelemetry;
    private int deletedNotifications;
    private int deletedReports;
    private int deletedAlerts;
    private int deletedAuditLogs;
    private double reclaimedMb;
    private long durationMs;
    private String executedAt;
    private String message;
}
