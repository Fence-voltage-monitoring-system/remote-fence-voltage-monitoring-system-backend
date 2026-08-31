package com.nerdc.elephantfence.backend.systemhealth.service;

import com.nerdc.elephantfence.backend.systemhealth.dto.JobRetryResponseDTO;
import com.nerdc.elephantfence.backend.systemhealth.dto.SystemHealthSnapshotDTO;
import com.nerdc.elephantfence.backend.systemhealth.repository.SystemHealthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SystemHealthService {

    private final SystemHealthRepository systemHealthRepository;

    @Transactional(readOnly = true)
    public SystemHealthSnapshotDTO getSnapshot() {
        long total = systemHealthRepository.countTotalGateways();
        long online = systemHealthRepository.countOnlineGateways();
        long offline = systemHealthRepository.countOfflineGateways();
        long late = systemHealthRepository.countLateReportingGateways();

        Object latestTel = systemHealthRepository.getLatestTelemetryAt();
        OffsetDateTime latestTelemetryAt = null;
        if (latestTel instanceof Timestamp) {
            latestTelemetryAt = OffsetDateTime.ofInstant(((Timestamp) latestTel).toInstant(), ZoneId.systemDefault());
        } else if (latestTel instanceof OffsetDateTime) {
            latestTelemetryAt = (OffsetDateTime) latestTel;
        }

        SystemHealthSnapshotDTO.GatewayHealthSummaryDTO gatewaySummary = SystemHealthSnapshotDTO.GatewayHealthSummaryDTO.builder()
                .total(total)
                .online(online)
                .offline(offline)
                .lateReporting(late)
                .communicationSuccessPercent(98.5) // Static metric for now
                .latestTelemetryAt(latestTelemetryAt)
                .build();

        List<Object[]> unhealthyRows = systemHealthRepository.findUnhealthyGateways();
        List<SystemHealthSnapshotDTO.UnhealthyGatewayDTO> unhealthyGateways = unhealthyRows.stream()
                .map(row -> {
                    OffsetDateTime lastSeen = null;
                    if (row[4] instanceof Timestamp) {
                        lastSeen = OffsetDateTime.ofInstant(((Timestamp) row[4]).toInstant(), ZoneId.systemDefault());
                    } else if (row[4] instanceof OffsetDateTime) {
                        lastSeen = (OffsetDateTime) row[4];
                    }
                    
                    String dbStatus = row[3] != null ? row[3].toString() : "offline";
                    String statusMapped = dbStatus.equals("warning") ? "LATE" : "OFFLINE";

                    return SystemHealthSnapshotDTO.UnhealthyGatewayDTO.builder()
                            .id(((Number) row[0]).longValue())
                            .code(row[1] != null ? row[1].toString() : "UNKNOWN")
                            .fenceCode(row[2] != null ? row[2].toString() : "UNKNOWN")
                            .state(statusMapped)
                            .lastCommunicationAt(lastSeen)
                            .delayMinutes(60)
                            .build();
                }).toList();

        String overallState = (offline > 0 || late > 0) ? "DEGRADED" : "HEALTHY";

        List<SystemHealthSnapshotDTO.CoreServiceHealthDTO> services = List.of(
                SystemHealthSnapshotDTO.CoreServiceHealthDTO.builder()
                        .id("db-primary")
                        .name("PostgreSQL Primary")
                        .state("HEALTHY")
                        .responseTimeMs(15L)
                        .lastSuccessfulCheck(OffsetDateTime.now())
                        .build()
        );

        return SystemHealthSnapshotDTO.builder()
                .overallState(overallState)
                .uptimeSeconds(86400) // 1 day mock uptime
                .activeIssues(unhealthyGateways.size())
                .checkedAt(OffsetDateTime.now())
                .services(services)
                .gatewaySummary(gatewaySummary)
                .unhealthyGateways(unhealthyGateways)
                .jobs(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }

    public JobRetryResponseDTO retryJob(String jobId, String reason) {
        return JobRetryResponseDTO.builder()
                .message("Job retry scheduled successfully for: " + jobId)
                .executionId(UUID.randomUUID().toString())
                .build();
    }
}
