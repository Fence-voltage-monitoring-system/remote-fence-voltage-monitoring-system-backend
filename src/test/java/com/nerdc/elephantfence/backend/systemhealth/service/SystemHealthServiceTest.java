package com.nerdc.elephantfence.backend.systemhealth.service;

import com.nerdc.elephantfence.backend.systemhealth.dto.JobRetryResponseDTO;
import com.nerdc.elephantfence.backend.systemhealth.dto.SystemHealthSnapshotDTO;
import com.nerdc.elephantfence.backend.systemhealth.repository.SystemHealthRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemHealthServiceTest {

    @Mock
    private SystemHealthRepository systemHealthRepository;

    @InjectMocks
    private SystemHealthService systemHealthService;

    private List<Object[]> emptyRowList() {
        return new ArrayList<>();
    }

    @Test
    void getSnapshot_shouldAggregateMetricsAndReturnHealthy() {
        when(systemHealthRepository.countTotalGateways()).thenReturn(10L);
        when(systemHealthRepository.countOnlineGateways()).thenReturn(10L);
        when(systemHealthRepository.countOfflineGateways()).thenReturn(0L);
        when(systemHealthRepository.countLateReportingGateways()).thenReturn(0L);
        when(systemHealthRepository.getLatestTelemetryAt()).thenReturn(OffsetDateTime.now());
        when(systemHealthRepository.findUnhealthyGateways()).thenReturn(emptyRowList());

        SystemHealthSnapshotDTO snapshot = systemHealthService.getSnapshot();

        assertThat(snapshot.getOverallState()).isEqualTo("HEALTHY");
        assertThat(snapshot.getGatewaySummary().getTotal()).isEqualTo(10L);
        assertThat(snapshot.getActiveIssues()).isEqualTo(0);
    }

    @Test
    void getSnapshot_shouldReturnDegradedWhenGatewaysOffline() {
        when(systemHealthRepository.countTotalGateways()).thenReturn(10L);
        when(systemHealthRepository.countOnlineGateways()).thenReturn(8L);
        when(systemHealthRepository.countOfflineGateways()).thenReturn(2L); // Triggers degraded
        when(systemHealthRepository.countLateReportingGateways()).thenReturn(0L);
        when(systemHealthRepository.getLatestTelemetryAt()).thenReturn(OffsetDateTime.now());

        Object[] unhealthyRow = new Object[]{ 1L, "SN-1", "FC-1", "offline", OffsetDateTime.now() };
        List<Object[]> rows = new ArrayList<>();
        rows.add(unhealthyRow);
        when(systemHealthRepository.findUnhealthyGateways()).thenReturn(rows);

        SystemHealthSnapshotDTO snapshot = systemHealthService.getSnapshot();

        assertThat(snapshot.getOverallState()).isEqualTo("DEGRADED");
        assertThat(snapshot.getActiveIssues()).isEqualTo(1);
    }

    @Test
    void retryJob_shouldReturnExecutionId() {
        JobRetryResponseDTO response = systemHealthService.retryJob("job-123", "Manual test");

        assertThat(response.getMessage()).contains("job-123");
        assertThat(response.getExecutionId()).isNotNull();
    }
}
