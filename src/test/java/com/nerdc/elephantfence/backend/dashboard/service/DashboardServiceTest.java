package com.nerdc.elephantfence.backend.dashboard.service;

import com.nerdc.elephantfence.backend.dashboard.dto.DashboardOverviewResponseDTO;
import com.nerdc.elephantfence.backend.dashboard.dto.DeviceAnalyticsResponseDTO;
import com.nerdc.elephantfence.backend.dashboard.repository.DashboardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @InjectMocks
    private DashboardService dashboardService;

    // Fake device row: device_id, voltage, battery, status, fence_id, fence_name, section_id
    private Object[] mockDeviceRow;

    @BeforeEach
    void setUp() {
        mockDeviceRow = new Object[]{
                1L,
                new BigDecimal("6.5"),
                80,
                "online",
                10L,
                "Northern Fence",
                20L
        };
    }

    private List<Object[]> rowList(Object[] row) {
        List<Object[]> list = new ArrayList<>();
        list.add(row);
        return list;
    }

    private List<Object[]> emptyRowList() {
        return new ArrayList<>();
    }

    // ─── Test 1: getOverview returns correct summary stats ────────────────────

    @Test
    void getOverview_shouldReturnCorrectSummaryStats() {
        when(dashboardRepository.countTotalFences()).thenReturn(5L);
        when(dashboardRepository.countTotalDevices()).thenReturn(20L);
        when(dashboardRepository.countActiveDevices()).thenReturn(15L);
        when(dashboardRepository.countCriticalAlerts()).thenReturn(3L);
        when(dashboardRepository.countLowVoltageFences()).thenReturn(2L);
        when(dashboardRepository.findFirstActiveDeviceContext()).thenReturn(rowList(mockDeviceRow));

        DashboardOverviewResponseDTO response = dashboardService.getOverview();

        assertThat(response).isNotNull();
        assertThat(response.getSummary().getTotalFences()).isEqualTo(5L);
        assertThat(response.getSummary().getTotalDevices()).isEqualTo(20L);
        assertThat(response.getSummary().getActiveDevices()).isEqualTo(15L);
        assertThat(response.getSummary().getCriticalAlerts()).isEqualTo(3L);
        assertThat(response.getSummary().getLowVoltageFences()).isEqualTo(2L);
    }

    // ─── Test 2: getOverview maps device status 'online' to 'healthy' ─────────

    @Test
    void getOverview_shouldMapOnlineStatusToHealthy() {
        when(dashboardRepository.countTotalFences()).thenReturn(1L);
        when(dashboardRepository.countTotalDevices()).thenReturn(1L);
        when(dashboardRepository.countActiveDevices()).thenReturn(1L);
        when(dashboardRepository.countCriticalAlerts()).thenReturn(0L);
        when(dashboardRepository.countLowVoltageFences()).thenReturn(0L);
        when(dashboardRepository.findFirstActiveDeviceContext()).thenReturn(rowList(mockDeviceRow));

        DashboardOverviewResponseDTO response = dashboardService.getOverview();

        assertThat(response.getSelectedDevice()).isNotNull();
        assertThat(response.getSelectedDevice().getStatus()).isEqualTo("healthy");
        assertThat(response.getSelectedDevice().getFenceName()).isEqualTo("Northern Fence");
    }

    // ─── Test 3: getOverview with no active devices returns null selectedDevice ─

    @Test
    void getOverview_shouldReturnNullSelectedDeviceWhenNoneActive() {
        when(dashboardRepository.countTotalFences()).thenReturn(0L);
        when(dashboardRepository.countTotalDevices()).thenReturn(0L);
        when(dashboardRepository.countActiveDevices()).thenReturn(0L);
        when(dashboardRepository.countCriticalAlerts()).thenReturn(0L);
        when(dashboardRepository.countLowVoltageFences()).thenReturn(0L);
        when(dashboardRepository.findFirstActiveDeviceContext()).thenReturn(emptyRowList());

        DashboardOverviewResponseDTO response = dashboardService.getOverview();

        assertThat(response.getSelectedDevice()).isNull();
    }

    // ─── Test 4: getDeviceAnalytics returns voltage history and alert counts ──

    @Test
    void getDeviceAnalytics_shouldReturnVoltageHistoryAndAlertCounts() {
        Object[] voltageRow = new Object[]{ OffsetDateTime.now(), new BigDecimal("6.5") };

        when(dashboardRepository.findDeviceContext(1L)).thenReturn(rowList(mockDeviceRow));
        when(dashboardRepository.findVoltageHistory(1L, 50)).thenReturn(rowList(voltageRow));
        when(dashboardRepository.countAlertsByDeviceAndSeverity(1L, "critical")).thenReturn(2L);
        when(dashboardRepository.countAlertsByDeviceAndSeverity(1L, "warning")).thenReturn(1L);
        when(dashboardRepository.countOfflineAlertsByDevice(1L)).thenReturn(0L);
        when(dashboardRepository.countResolvedAlertsByDevice(1L)).thenReturn(5L);

        DeviceAnalyticsResponseDTO response = dashboardService.getDeviceAnalytics("1");

        assertThat(response).isNotNull();
        assertThat(response.getVoltageHistory()).hasSize(1);
        assertThat(response.getVoltageHistory().get(0).getVoltage()).isEqualTo(6.5);
        assertThat(response.getAlertCounts().getCritical()).isEqualTo(2L);
        assertThat(response.getAlertCounts().getWarning()).isEqualTo(1L);
        assertThat(response.getAlertCounts().getResolved()).isEqualTo(5L);
    }

    // ─── Test 5: getDeviceAnalytics throws when device not found ─────────────

    @Test
    void getDeviceAnalytics_shouldThrowWhenDeviceNotFound() {
        when(dashboardRepository.findDeviceContext(99L)).thenReturn(emptyRowList());

        assertThatThrownBy(() -> dashboardService.getDeviceAnalytics("99"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Device not found with ID: 99");
    }
}
