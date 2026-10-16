package com.nerdc.elephantfence.backend.configuration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nerdc.elephantfence.backend.configuration.dto.RetentionCleanupResultDTO;
import com.nerdc.elephantfence.backend.configuration.dto.RetentionStatusDTO;
import com.nerdc.elephantfence.backend.configuration.entity.SystemConfiguration;
import com.nerdc.elephantfence.backend.configuration.repository.SystemConfigurationRepository;
import com.nerdc.elephantfence.backend.health.service.SystemHealthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataRetentionServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private SystemConfigurationRepository configRepository;

    @Mock
    private SystemHealthService healthService;

    @InjectMocks
    private DataRetentionService retentionService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SystemConfiguration retentionConfig;

    @BeforeEach
    void setUp() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("rawTelemetryDays", 30);
        node.put("notificationDays", 14);
        node.put("generatedReportDays", 90);
        node.put("alertIncidentDays", 180);
        node.put("auditLogDays", 365);
        node.put("archiveBeforeDeletion", true);
        node.put("automaticCleanupEnabled", true);
        node.put("cleanupSchedule", "DAILY");
        node.put("cleanupTime", "02:00");

        retentionConfig = SystemConfiguration.builder()
                .section("retention")
                .configData(node)
                .version(1)
                .build();

        lenient().when(configRepository.findBySection("retention")).thenReturn(Optional.of(retentionConfig));
        lenient().when(configRepository.findBySection("general")).thenReturn(Optional.empty());
        lenient().when(configRepository.findBySection("retention-status")).thenReturn(Optional.empty());
    }

    @Test
    void getStatus_returnsDynamicMetricsAndFormattedNextRun() {
        when(jdbcTemplate.queryForObject(eq("SELECT pg_database_size(current_database())"), eq(Long.class)))
                .thenReturn(70L * 1024 * 1024 * 1024);

        RetentionStatusDTO status = retentionService.getStatus();

        assertThat(status).isNotNull();
        assertThat(status.getDatabaseSizeFormatted()).isEqualTo("70.0 GB");
        assertThat(status.getDatabaseAllocatedFormatted()).isEqualTo("100.0 GB");
        assertThat(status.getDatabaseUsagePercentage()).isEqualTo(70);
        assertThat(status.isAutomaticCleanupEnabled()).isTrue();
        assertThat(status.getNextCleanupStatus()).isEqualTo("Scheduled");
        assertThat(status.getNextCleanupFormatted()).contains("· 02:00");
    }

    @Test
    void executeCleanup_prunesExpiredRecordsAndPersistsMetadata() {
        when(jdbcTemplate.update(contains("DELETE FROM telemetry_readings"), any(OffsetDateTime.class))).thenReturn(20);
        when(jdbcTemplate.update(contains("DELETE FROM user_notifications"), any(OffsetDateTime.class))).thenReturn(15);
        when(jdbcTemplate.update(contains("DELETE FROM generated_reports"), any(OffsetDateTime.class))).thenReturn(2);
        when(jdbcTemplate.update(contains("DELETE FROM alerts"), any(OffsetDateTime.class), any(OffsetDateTime.class))).thenReturn(5);
        when(jdbcTemplate.update(contains("DELETE FROM audit_logs"), any(OffsetDateTime.class))).thenReturn(8);

        RetentionCleanupResultDTO result = retentionService.executeCleanup(true, "Manual administrative test cleanup");

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isManual()).isTrue();
        assertThat(result.getTotalRecordsDeleted()).isEqualTo(50);
        assertThat(result.getDeletedTelemetry()).isEqualTo(20);
        assertThat(result.getDeletedNotifications()).isEqualTo(15);
        assertThat(result.getDeletedReports()).isEqualTo(2);
        assertThat(result.getDeletedAlerts()).isEqualTo(5);
        assertThat(result.getDeletedAuditLogs()).isEqualTo(8);

        verify(configRepository).save(argThat(cfg -> "retention-status".equals(cfg.getSection())));
        verify(healthService).recordJobRun(eq("retention-cleanup"), eq("SUCCESS"), any(), any(), anyLong());
    }
}
