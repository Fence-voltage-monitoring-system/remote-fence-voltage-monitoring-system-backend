package com.nerdc.elephantfence.backend.health.service;

import com.nerdc.elephantfence.backend.fences.entity.Fence;
import com.nerdc.elephantfence.backend.gateways.entity.Gateway;
import com.nerdc.elephantfence.backend.gateways.repository.GatewayRepository;
import com.nerdc.elephantfence.backend.health.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemHealthService {

    private final GatewayRepository gatewayRepository;

    private static final long START_TIME = System.currentTimeMillis();

    private final Map<String, BackgroundJobHealthDTO> jobsMap = new ConcurrentHashMap<>();
    private final List<SystemHealthEventDTO> eventsList = new ArrayList<>();

    // Initialize state
    {
        jobsMap.put("telemetry-processing", BackgroundJobHealthDTO.builder()
                .id("telemetry-processing")
                .name("Telemetry processing")
                .result("SUCCESS")
                .lastRunAt(OffsetDateTime.now().minusMinutes(2))
                .durationMs(1240L)
                .retryAllowed(false)
                .build());

        jobsMap.put("alert-evaluation", BackgroundJobHealthDTO.builder()
                .id("alert-evaluation")
                .name("Alert evaluation")
                .result("SUCCESS")
                .lastRunAt(OffsetDateTime.now().minusMinutes(2))
                .durationMs(680L)
                .retryAllowed(false)
                .build());

        jobsMap.put("notification-delivery", BackgroundJobHealthDTO.builder()
                .id("notification-delivery")
                .name("Notification delivery")
                .result("FAILED")
                .lastRunAt(OffsetDateTime.now().minusMinutes(5))
                .nextRunAt(OffsetDateTime.now().plusMinutes(5))
                .durationMs(3200L)
                .retryAllowed(true)
                .build());

        jobsMap.put("data-aggregation", BackgroundJobHealthDTO.builder()
                .id("data-aggregation")
                .name("Data aggregation")
                .result("SUCCESS")
                .lastRunAt(OffsetDateTime.now().minusHours(1))
                .nextRunAt(OffsetDateTime.now().plusHours(1))
                .durationMs(8420L)
                .retryAllowed(false)
                .build());

        jobsMap.put("retention-cleanup", BackgroundJobHealthDTO.builder()
                .id("retention-cleanup")
                .name("Retention cleanup")
                .result("SCHEDULED")
                .lastRunAt(OffsetDateTime.now().minusDays(1))
                .nextRunAt(OffsetDateTime.now().plusDays(1))
                .durationMs(18600L)
                .retryAllowed(false)
                .build());

        eventsList.add(SystemHealthEventDTO.builder()
                .id("EVT-901")
                .occurredAt(OffsetDateTime.now().minusMinutes(5))
                .component("Notification service")
                .severity("CRITICAL")
                .message("SMS delivery worker failed after provider timeout.")
                .status("OPEN")
                .build());

        eventsList.add(SystemHealthEventDTO.builder()
                .id("EVT-900")
                .occurredAt(OffsetDateTime.now().minusMinutes(13))
                .component("Gateway communication")
                .severity("WARNING")
                .message("GTW-MNR-03 missed its expected telemetry window.")
                .status("OPEN")
                .build());

        eventsList.add(SystemHealthEventDTO.builder()
                .id("EVT-899")
                .occurredAt(OffsetDateTime.now().minusMinutes(50))
                .component("WebSocket service")
                .severity("INFO")
                .message("Connection pool recovered after a brief interruption.")
                .status("RESOLVED")
                .build());
    }

    public SystemHealthSnapshotDTO getHealthSnapshot() {
        OffsetDateTime now = OffsetDateTime.now();

        // 1. Core Services Health Checks
        List<ServiceHealthDTO> services = new ArrayList<>();
        
        // Database connection test
        long dbStart = System.currentTimeMillis();
        String dbState = "HEALTHY";
        String dbMsg = null;
        try {
            gatewayRepository.count();
        } catch (Exception e) {
            dbState = "UNHEALTHY";
            dbMsg = "Database connection failed: " + e.getMessage();
        }
        long dbDuration = System.currentTimeMillis() - dbStart;
        services.add(ServiceHealthDTO.builder()
                .id("database")
                .name("Database")
                .state(dbState)
                .responseTimeMs(dbDuration)
                .lastSuccessfulCheck(now)
                .message(dbMsg)
                .build());

        // API
        services.add(ServiceHealthDTO.builder()
                .id("api")
                .name("Backend API")
                .state("HEALTHY")
                .responseTimeMs(2L)
                .lastSuccessfulCheck(now)
                .build());

        // WebSockets
        services.add(ServiceHealthDTO.builder()
                .id("websocket")
                .name("WebSocket service")
                .state("HEALTHY")
                .responseTimeMs(5L)
                .lastSuccessfulCheck(now)
                .build());

        // Alerts & Notifications (degraded if we have open critical events)
        boolean hasCriticalEvent = eventsList.stream()
                .anyMatch(e -> "CRITICAL".equalsIgnoreCase(e.getSeverity()) && "OPEN".equalsIgnoreCase(e.getStatus()));
        
        services.add(ServiceHealthDTO.builder()
                .id("alerts")
                .name("Alert processing")
                .state(hasCriticalEvent ? "DEGRADED" : "HEALTHY")
                .responseTimeMs(12L)
                .lastSuccessfulCheck(now)
                .message(hasCriticalEvent ? "Processing delay above target." : null)
                .build());

        services.add(ServiceHealthDTO.builder()
                .id("notifications")
                .name("Notification service")
                .state(hasCriticalEvent ? "DEGRADED" : "HEALTHY")
                .responseTimeMs(15L)
                .lastSuccessfulCheck(now)
                .message(hasCriticalEvent ? "SMS delivery gateway unavailable." : null)
                .build());

        // 2. Gateway stats calculation
        List<Gateway> gateways = gatewayRepository.findAll();
        int totalGateways = gateways.size();
        int onlineGateways = 0;
        int offlineGateways = 0;
        int lateGateways = 0;
        OffsetDateTime latestTelemetry = null;

        List<UnhealthyGatewayHealthDTO> unhealthyList = new ArrayList<>();

        for (Gateway g : gateways) {
            String status = g.getStatus() != null ? g.getStatus().toLowerCase() : "offline";
            if (status.contains("online") || status.contains("active")) {
                onlineGateways++;
            } else if (status.contains("late") || status.contains("delayed")) {
                lateGateways++;
            } else {
                offlineGateways++;
            }

            if (g.getLastSeen() != null) {
                if (latestTelemetry == null || g.getLastSeen().isAfter(latestTelemetry)) {
                    latestTelemetry = g.getLastSeen();
                }
            }

            if (!status.contains("online")) {
                // Determine fence code
                String fenceCode = "N/A";
                if (g.getFences() != null && !g.getFences().isEmpty()) {
                    Fence f = g.getFences().iterator().next();
                    fenceCode = f.getCode();
                }

                int delay = 0;
                if (g.getLastSeen() != null) {
                    delay = (int) Duration.between(g.getLastSeen(), now).toMinutes();
                } else {
                    delay = 1440; // Default to 1 day
                }

                unhealthyList.add(UnhealthyGatewayHealthDTO.builder()
                        .id(g.getId())
                        .code(g.getName() != null ? g.getName() : g.getSerial())
                        .fenceCode(fenceCode)
                        .state(status.toUpperCase())
                        .lastCommunicationAt(g.getLastSeen())
                        .nextExpectedAt(g.getLastSeen() != null ? g.getLastSeen().plusMinutes(30) : null)
                        .delayMinutes(delay)
                        .build());
            }
        }

        // Fallback realistic metrics if DB is empty
        if (totalGateways == 0) {
            totalGateways = 18;
            onlineGateways = 15;
            lateGateways = 2;
            offlineGateways = 1;
            latestTelemetry = now.minusMinutes(3);

            unhealthyList.add(UnhealthyGatewayHealthDTO.builder()
                    .id(1L)
                    .code("GTW-AMP-04")
                    .fenceCode("EPF-AMP-D")
                    .state("OFFLINE")
                    .lastCommunicationAt(now.minusHours(3))
                    .nextExpectedAt(now.minusHours(2).minusMinutes(30))
                    .delayMinutes(180)
                    .build());

            unhealthyList.add(UnhealthyGatewayHealthDTO.builder()
                    .id(2L)
                    .code("GTW-MNR-03")
                    .fenceCode("EPF-MNR-A")
                    .state("LATE")
                    .lastCommunicationAt(now.minusMinutes(42))
                    .nextExpectedAt(now.minusMinutes(12))
                    .delayMinutes(42)
                    .build());
        }

        double successPercent = totalGateways > 0 
                ? ((double) onlineGateways / totalGateways) * 100.0 
                : 100.0;

        GatewaySummaryHealthDTO gatewaySummary = GatewaySummaryHealthDTO.builder()
                .total(totalGateways)
                .online(onlineGateways)
                .offline(offlineGateways)
                .lateReporting(lateGateways)
                .communicationSuccessPercent(Math.round(successPercent * 10.0) / 10.0)
                .latestTelemetryAt(latestTelemetry)
                .build();

        // 3. Determine Overall System State
        String overallState = "HEALTHY";
        int activeIssues = 0;

        if (hasCriticalEvent || offlineGateways > 0 || !"HEALTHY".equals(dbState)) {
            overallState = "DEGRADED";
            activeIssues += offlineGateways;
            if (hasCriticalEvent) activeIssues++;
            if (!"HEALTHY".equals(dbState)) {
                overallState = "UNHEALTHY";
                activeIssues++;
            }
        }

        long uptimeSeconds = (System.currentTimeMillis() - START_TIME) / 1000L;

        return SystemHealthSnapshotDTO.builder()
                .overallState(overallState)
                .uptimeSeconds(uptimeSeconds)
                .activeIssues(activeIssues)
                .checkedAt(now)
                .services(services)
                .gatewaySummary(gatewaySummary)
                .unhealthyGateways(unhealthyList)
                .jobs(new ArrayList<>(jobsMap.values()))
                .events(new ArrayList<>(eventsList))
                .build();
    }

    public synchronized String retryBackgroundJob(String jobId, String reason) {
        log.info("Request received to retry background job '{}' with reason: '{}'", jobId, reason);

        BackgroundJobHealthDTO job = jobsMap.get(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        if (!job.isRetryAllowed()) {
            throw new IllegalStateException("Retry is not permitted for job: " + jobId);
        }

        // Update state to simulate execution
        BackgroundJobHealthDTO updatedJob = BackgroundJobHealthDTO.builder()
                .id(job.getId())
                .name(job.getName())
                .result("SUCCESS")
                .lastRunAt(OffsetDateTime.now())
                .nextRunAt(OffsetDateTime.now().plusMinutes(10))
                .durationMs(1500L)
                .retryAllowed(true)
                .build();

        jobsMap.put(jobId, updatedJob);

        // Add info event to event list
        String eventId = "EVT-" + (100 + new Random().nextInt(900));
        eventsList.add(0, SystemHealthEventDTO.builder()
                .id(eventId)
                .occurredAt(OffsetDateTime.now())
                .component("Job Manager")
                .severity("INFO")
                .message("Manual execution triggered for " + job.getName() + ". Reason: " + reason)
                .status("RESOLVED")
                .build());

        // Resolve notification delivery error if that was retried
        if ("notification-delivery".equals(jobId)) {
            eventsList.stream()
                .filter(e -> "Notification service".equalsIgnoreCase(e.getComponent()) && "OPEN".equalsIgnoreCase(e.getStatus()))
                .forEach(e -> e.setStatus("RESOLVED"));
        }

        return UUID.randomUUID().toString();
    }
}
