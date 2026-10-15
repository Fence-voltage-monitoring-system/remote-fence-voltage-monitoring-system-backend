package com.nerdc.elephantfence.backend.telemetry.service;

import com.nerdc.elephantfence.backend.configuration.service.ConfigurationService;
import com.nerdc.elephantfence.backend.fences.repository.FenceRepository;
import com.nerdc.elephantfence.backend.sections.repository.SectionRepository;
import com.nerdc.elephantfence.backend.devices.entity.Device;
import com.nerdc.elephantfence.backend.devices.entity.DeviceStatus;
import com.nerdc.elephantfence.backend.devices.repository.DeviceRepository;
import com.nerdc.elephantfence.backend.telemetry.dto.TelemetryIngestRequestDTO;
import com.nerdc.elephantfence.backend.telemetry.dto.TelemetryResponseDTO;
import com.nerdc.elephantfence.backend.telemetry.entity.TelemetryReading;
import com.nerdc.elephantfence.backend.telemetry.repository.TelemetryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock
    private TelemetryRepository telemetryRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private FenceRepository fenceRepository;

    @Mock
    private ConfigurationService configurationService;

    @InjectMocks
    private TelemetryService telemetryService;

    private Device device;
    private TelemetryReading reading;

    @BeforeEach
    void setUp() {
        lenient().when(configurationService.getVoltageThresholdConfig()).thenReturn(
                new ConfigurationService.VoltageThresholdConfig(5.0, 3.0, 1.5, 20.0)
        );
        device = Device.builder()
                .id(1L)
                .name("Voltage Monitor 1")
                .serial("SN-12345")
                .status(DeviceStatus.offline)
                .voltage(0.0)
                .battery(0)
                .signal(0)
                .enabled(true)
                .build();

        reading = TelemetryReading.builder()
                .id(100L)
                .deviceId(1L)
                .voltageKv(new BigDecimal("6.5"))
                .battery(80)
                .signal(90)
                .recordedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void ingestTelemetry_shouldSaveReadingAndUpdateDeviceHealth() {
        TelemetryIngestRequestDTO dto = TelemetryIngestRequestDTO.builder()
                .deviceSerial("SN-12345")
                .voltage(new BigDecimal("6.5"))
                .battery(80)
                .signal(90)
                .build();

        when(deviceRepository.findBySerialIgnoreCase("SN-12345")).thenReturn(Optional.of(device));
        when(telemetryRepository.save(any(TelemetryReading.class))).thenReturn(reading);

        TelemetryResponseDTO response = telemetryService.ingestTelemetry(dto);

        assertThat(response).isNotNull();
        assertThat(response.getVoltageKv()).isEqualTo(new BigDecimal("6.5"));
        assertThat(response.getBattery()).isEqualTo(80);
        assertThat(response.getSignal()).isEqualTo(90);

        // Verify device values were updated
        assertThat(device.getVoltage()).isEqualTo(6.5);
        assertThat(device.getBattery()).isEqualTo(80);
        assertThat(device.getSignal()).isEqualTo(90);
        assertThat(device.getStatus()).isEqualTo(DeviceStatus.online);
        assertThat(device.getLastSeen()).isNotNull();

        verify(deviceRepository, times(1)).save(device);
        verify(telemetryRepository, times(1)).save(any(TelemetryReading.class));
    }

    @Test
    void ingestTelemetry_shouldSetWarningStatusWhenVoltageLow() {
        TelemetryIngestRequestDTO dto = TelemetryIngestRequestDTO.builder()
                .deviceSerial("SN-12345")
                .voltage(new BigDecimal("4.2")) // < 5.0 kV
                .battery(40)
                .signal(50)
                .build();

        TelemetryReading lowReading = TelemetryReading.builder()
                .id(101L)
                .deviceId(1L)
                .voltageKv(new BigDecimal("4.2"))
                .recordedAt(OffsetDateTime.now())
                .build();

        when(deviceRepository.findBySerialIgnoreCase("SN-12345")).thenReturn(Optional.of(device));
        when(telemetryRepository.save(any(TelemetryReading.class))).thenReturn(lowReading);

        telemetryService.ingestTelemetry(dto);

        assertThat(device.getStatus()).isEqualTo(DeviceStatus.warning);
        verify(deviceRepository, times(1)).save(device);
    }

    @Test
    void ingestTelemetry_shouldThrowExceptionWhenDeviceNotFound() {
        TelemetryIngestRequestDTO dto = TelemetryIngestRequestDTO.builder()
                .deviceSerial("SN-UNKNOWN")
                .voltage(new BigDecimal("6.0"))
                .build();

        when(deviceRepository.findBySerialIgnoreCase("SN-UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> telemetryService.ingestTelemetry(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Device not found with serial");
    }

    @Test
    void getDeviceHistory_shouldReturnHistoryList() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(telemetryRepository.findByDeviceIdOrderByRecordedAtDesc(eq(1L), any(PageRequest.class)))
                .thenReturn(List.of(reading));

        List<TelemetryResponseDTO> history = telemetryService.getDeviceHistory(1L, 10);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getDeviceSerial()).isEqualTo("SN-12345");
        assertThat(history.get(0).getVoltageKv()).isEqualTo(new BigDecimal("6.5"));
    }

    @Test
    void ingestTelemetry_shouldUpdateSectionAndFence_basedOnConfiguredThresholds() {
        device.setSectionId(10L);
        com.nerdc.elephantfence.backend.sections.entity.Section section =
                com.nerdc.elephantfence.backend.sections.entity.Section.builder()
                        .id(10L)
                        .fenceId(20L)
                        .code("SEC-01")
                        .status(com.nerdc.elephantfence.backend.sections.entity.SectionStatus.OFFLINE)
                        .build();

        com.nerdc.elephantfence.backend.fences.entity.Fence fence =
                com.nerdc.elephantfence.backend.fences.entity.Fence.builder()
                        .id(20L)
                        .code("FNC-01")
                        .name("Test Fence")
                        .health(com.nerdc.elephantfence.backend.fences.entity.FenceHealth.OFFLINE)
                        .build();

        TelemetryIngestRequestDTO dto = TelemetryIngestRequestDTO.builder()
                .deviceSerial("SN-12345")
                .voltage(new BigDecimal("1.2")) // < criticalKv (1.5)
                .battery(85)
                .build();

        TelemetryReading readingCritical = TelemetryReading.builder()
                .id(102L)
                .deviceId(1L)
                .voltageKv(new BigDecimal("1.2"))
                .build();

        when(deviceRepository.findBySerialIgnoreCase("SN-12345")).thenReturn(Optional.of(device));
        when(telemetryRepository.save(any(TelemetryReading.class))).thenReturn(readingCritical);
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(fenceRepository.findById(20L)).thenReturn(Optional.of(fence));
        when(sectionRepository.findByFenceIdOrderByCodeAsc(20L)).thenReturn(List.of(section));

        telemetryService.ingestTelemetry(dto);

        assertThat(section.getStatus()).isEqualTo(com.nerdc.elephantfence.backend.sections.entity.SectionStatus.CRITICAL);
        assertThat(section.getVoltageKv()).isEqualByComparingTo("1.2");
        assertThat(fence.getHealth()).isEqualTo(com.nerdc.elephantfence.backend.fences.entity.FenceHealth.CRITICAL);
        assertThat(fence.getAverageVoltageKv()).isEqualByComparingTo("1.2");
        verify(sectionRepository).save(section);
        verify(fenceRepository).save(fence);
    }
}
