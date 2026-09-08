package com.nerdc.elephantfence.backend.dashboard.service;

import com.nerdc.elephantfence.backend.dashboard.dto.DashboardOverviewResponseDTO;
import com.nerdc.elephantfence.backend.dashboard.dto.DashboardSummaryDTO;
import com.nerdc.elephantfence.backend.dashboard.dto.DeviceAnalyticsResponseDTO;
import com.nerdc.elephantfence.backend.dashboard.dto.DeviceMonitoringContextDTO;
import com.nerdc.elephantfence.backend.dashboard.dto.VoltageReadingDTO;
import com.nerdc.elephantfence.backend.dashboard.repository.DashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    // ─── Dashboard Overview ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardOverviewResponseDTO getOverview() {
        // 1. Build summary stats
        DashboardSummaryDTO summary = DashboardSummaryDTO.builder()
                .totalFences(dashboardRepository.countTotalFences())
                .totalDevices(dashboardRepository.countTotalDevices())
                .activeDevices(dashboardRepository.countActiveDevices())
                .criticalAlerts(dashboardRepository.countCriticalAlerts())
                .lowVoltageFences(dashboardRepository.countLowVoltageFences())
                .build();

        // 2. Get most recently active device context
        List<Object[]> rows = dashboardRepository.findFirstActiveDeviceContext();
        DeviceMonitoringContextDTO selectedDevice = rows.isEmpty() ? null : mapToDeviceContext(rows.get(0));

        return DashboardOverviewResponseDTO.builder()
                .summary(summary)
                .selectedDevice(selectedDevice)
                .build();
    }

    // ─── Device Analytics ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DeviceAnalyticsResponseDTO getDeviceAnalytics(String deviceId) {
        Long id = null;
        try {
            id = Long.parseLong(deviceId);
        } catch (NumberFormatException e) {
            List<Object[]> firstActive = dashboardRepository.findFirstActiveDeviceContext();
            if (!firstActive.isEmpty()) {
                id = ((Number) firstActive.get(0)[0]).longValue();
            }
        }

        List<Object[]> contextRows = id != null ? dashboardRepository.findDeviceContext(id) : List.of();
        if (contextRows.isEmpty()) {
            contextRows = dashboardRepository.findFirstActiveDeviceContext();
        }

        if (contextRows.isEmpty()) {
            DeviceMonitoringContextDTO fallbackContext = DeviceMonitoringContextDTO.builder()
                    .deviceId(deviceId != null ? deviceId : "1")
                    .status("healthy")
                    .voltage(6.0)
                    .battery(85)
                    .fenceId("1")
                    .fenceName("Monaragala Elephant Protection Fence")
                    .sectionId("1")
                    .build();

            return DeviceAnalyticsResponseDTO.builder()
                    .device(fallbackContext)
                    .voltageHistory(List.of())
                    .alertCounts(DeviceAnalyticsResponseDTO.AlertCountsDTO.builder().build())
                    .build();
        }

        Long actualId = ((Number) contextRows.get(0)[0]).longValue();
        DeviceMonitoringContextDTO deviceContext = mapToDeviceContext(contextRows.get(0));

        // Fetch voltage history (last 50 readings for chart)
        List<VoltageReadingDTO> voltageHistory = dashboardRepository.findVoltageHistory(actualId, 50).stream()
                .map(row -> VoltageReadingDTO.builder()
                        .recordedAt((OffsetDateTime) row[0])
                        .voltage(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0)
                        .build())
                .toList();

        // Build alert counts
        DeviceAnalyticsResponseDTO.AlertCountsDTO alertCounts = DeviceAnalyticsResponseDTO.AlertCountsDTO.builder()
                .critical(dashboardRepository.countAlertsByDeviceAndSeverity(actualId, "critical"))
                .warning(dashboardRepository.countAlertsByDeviceAndSeverity(actualId, "warning"))
                .offline(dashboardRepository.countOfflineAlertsByDevice(actualId))
                .resolved(dashboardRepository.countResolvedAlertsByDevice(actualId))
                .build();

        return DeviceAnalyticsResponseDTO.builder()
                .device(deviceContext)
                .voltageHistory(voltageHistory)
                .alertCounts(alertCounts)
                .build();
    }

    // ─── Helper Mapper ─────────────────────────────────────────────────────────

    private DeviceMonitoringContextDTO mapToDeviceContext(Object[] row) {
        // row[0]=device_id, row[1]=voltage, row[2]=battery, row[3]=status,
        // row[4]=fence_id, row[5]=fence_name, row[6]=section_id
        String dbStatus = row[3] != null ? row[3].toString() : "offline";

        return DeviceMonitoringContextDTO.builder()
                .deviceId(row[0].toString())
                .voltage(row[1] != null ? ((Number) row[1]).doubleValue() : null)
                .battery(row[2] != null ? ((Number) row[2]).intValue() : null)
                .status(mapStatus(dbStatus))   // Map 'online' -> 'healthy' for frontend
                .fenceId(row[4] != null ? row[4].toString() : null)
                .fenceName(row[5] != null ? row[5].toString() : null)
                .sectionId(row[6] != null ? row[6].toString() : null)
                .build();
    }

    private String mapStatus(String dbStatus) {
        return switch (dbStatus) {
            case "online"  -> "healthy";   // Frontend expects 'healthy' not 'online'
            case "warning" -> "warning";
            case "offline" -> "offline";
            default        -> "offline";
        };
    }
}
