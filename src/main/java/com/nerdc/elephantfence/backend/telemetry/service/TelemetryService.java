package com.nerdc.elephantfence.backend.telemetry.service;

import com.nerdc.elephantfence.backend.configuration.service.ConfigurationService;
import com.nerdc.elephantfence.backend.devices.entity.Device;
import com.nerdc.elephantfence.backend.devices.entity.DeviceStatus;
import com.nerdc.elephantfence.backend.devices.repository.DeviceRepository;
import com.nerdc.elephantfence.backend.fences.entity.Fence;
import com.nerdc.elephantfence.backend.fences.entity.FenceHealth;
import com.nerdc.elephantfence.backend.fences.repository.FenceRepository;
import com.nerdc.elephantfence.backend.sections.entity.Section;
import com.nerdc.elephantfence.backend.sections.entity.SectionStatus;
import com.nerdc.elephantfence.backend.sections.repository.SectionRepository;
import com.nerdc.elephantfence.backend.telemetry.dto.TelemetryIngestRequestDTO;
import com.nerdc.elephantfence.backend.telemetry.dto.TelemetryResponseDTO;
import com.nerdc.elephantfence.backend.telemetry.entity.TelemetryReading;
import com.nerdc.elephantfence.backend.telemetry.repository.TelemetryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;
    private final DeviceRepository deviceRepository;
    private final SectionRepository sectionRepository;
    private final FenceRepository fenceRepository;
    private final ConfigurationService configurationService;

    @Transactional
    public TelemetryResponseDTO ingestTelemetry(TelemetryIngestRequestDTO dto) {
        // 1. Look up the device by its hardware serial number
        Device device = deviceRepository.findBySerialIgnoreCase(dto.getDeviceSerial())
                .orElseThrow(() -> new IllegalArgumentException("Device not found with serial: " + dto.getDeviceSerial()));

        // 2. Create and save the telemetry record
        TelemetryReading reading = TelemetryReading.builder()
                .deviceId(device.getId())
                .voltageKv(dto.getVoltage())
                .battery(dto.getBattery())
                .signal(dto.getSignal())
                .build();

        TelemetryReading savedReading = telemetryRepository.save(reading);

        // 3. Resolve active dynamic voltage and battery thresholds
        ConfigurationService.VoltageThresholdConfig voltageConfig = configurationService != null
                ? configurationService.getVoltageThresholdConfig()
                : new ConfigurationService.VoltageThresholdConfig(5.0, 3.0, 1.5, 20.0);

        double v = dto.getVoltage() != null ? dto.getVoltage().doubleValue() : 0.0;
        Integer battery = dto.getBattery();

        // 4. Update active device status metrics
        device.setVoltage(v);
        if (battery != null) device.setBattery(battery);
        if (dto.getSignal() != null) device.setSignal(dto.getSignal());
        device.setLastSeen(OffsetDateTime.now());

        // Derive online status based on voltage threshold and low battery
        DeviceStatus status = DeviceStatus.online;
        if (v < voltageConfig.healthyKv() || (battery != null && battery < voltageConfig.lowBatteryPercent())) {
            status = DeviceStatus.warning;
        }
        device.setStatus(status);
        deviceRepository.save(device);

        // 5. Update section telemetry and health classification if device is assigned to a section
        if (device.getSectionId() != null && sectionRepository != null) {
            sectionRepository.findById(device.getSectionId()).ifPresent(section -> {
                section.setVoltageKv(BigDecimal.valueOf(v));
                if (battery != null) section.setBattery(battery);

                SectionStatus sectionStatus;
                if (v < voltageConfig.criticalKv()) {
                    sectionStatus = SectionStatus.CRITICAL;
                } else if (v < voltageConfig.warningKv() || (battery != null && battery < voltageConfig.lowBatteryPercent())) {
                    sectionStatus = SectionStatus.WARNING;
                } else if (v >= voltageConfig.healthyKv()) {
                    sectionStatus = SectionStatus.HEALTHY;
                } else {
                    sectionStatus = SectionStatus.WARNING;
                }
                section.setStatus(sectionStatus);
                sectionRepository.save(section);

                // 6. Update overall fence health & average voltage
                if (section.getFenceId() != null && fenceRepository != null) {
                    fenceRepository.findById(section.getFenceId()).ifPresent(fence -> {
                        List<Section> fenceSections = sectionRepository.findByFenceIdOrderByCodeAsc(fence.getId());
                        double sumVoltage = 0.0;
                        int count = 0;
                        boolean hasCritical = false;
                        boolean hasWarning = false;
                        for (Section s : fenceSections) {
                            if (s.getVoltageKv() != null) {
                                sumVoltage += s.getVoltageKv().doubleValue();
                                count++;
                            }
                            if (s.getStatus() == SectionStatus.CRITICAL) {
                                hasCritical = true;
                            } else if (s.getStatus() == SectionStatus.WARNING) {
                                hasWarning = true;
                            }
                        }
                        if (count > 0) {
                            double avg = sumVoltage / count;
                            fence.setAverageVoltageKv(BigDecimal.valueOf(Math.round(avg * 100.0) / 100.0));
                        }
                        if (hasCritical) {
                            fence.setHealth(FenceHealth.CRITICAL);
                        } else if (hasWarning) {
                            fence.setHealth(FenceHealth.WARNING);
                        } else {
                            fence.setHealth(FenceHealth.HEALTHY);
                        }
                        fenceRepository.save(fence);
                    });
                }
            });
        }

        // 7. Return receipt
        return convertToResponseDTO(savedReading, dto.getDeviceSerial());
    }

    @Transactional(readOnly = true)
    public List<TelemetryResponseDTO> getDeviceHistory(Long deviceId, int limit) {
        String deviceSerial = deviceRepository.findById(deviceId)
                .map(Device::getSerial)
                .orElse("unknown");

        return telemetryRepository.findByDeviceIdOrderByRecordedAtDesc(deviceId, PageRequest.of(0, limit)).stream()
                .map(reading -> convertToResponseDTO(reading, deviceSerial))
                .toList();
    }

    private TelemetryResponseDTO convertToResponseDTO(TelemetryReading reading, String serial) {
        return TelemetryResponseDTO.builder()
                .id(reading.getId().toString())
                .deviceSerial(serial)
                .voltageKv(reading.getVoltageKv())
                .battery(reading.getBattery())
                .signal(reading.getSignal())
                .recordedAt(reading.getRecordedAt())
                .build();
    }
}
