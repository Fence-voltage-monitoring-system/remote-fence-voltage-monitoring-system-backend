package com.nerdc.elephantfence.backend.telemetry.service;

import com.nerdc.elephantfence.backend.devices.entity.Device;
import com.nerdc.elephantfence.backend.devices.entity.DeviceStatus;
import com.nerdc.elephantfence.backend.devices.repository.DeviceRepository;
import com.nerdc.elephantfence.backend.telemetry.dto.TelemetryIngestRequestDTO;
import com.nerdc.elephantfence.backend.telemetry.dto.TelemetryResponseDTO;
import com.nerdc.elephantfence.backend.telemetry.entity.TelemetryReading;
import com.nerdc.elephantfence.backend.telemetry.repository.TelemetryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;
    private final DeviceRepository deviceRepository;

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

        // 3. Update active device status metrics
        device.setVoltage(dto.getVoltage().doubleValue());
        if (dto.getBattery() != null) device.setBattery(dto.getBattery());
        if (dto.getSignal() != null) device.setSignal(dto.getSignal());
        device.setLastSeen(OffsetDateTime.now());

        // Derive online status based on voltage threshold (warning if voltage drops below 5.0 kV)
        DeviceStatus status = DeviceStatus.online;
        if (dto.getVoltage().doubleValue() < 5.0) {
            status = DeviceStatus.warning;
        }
        device.setStatus(status);

        deviceRepository.save(device);

        // 4. Return receipt
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
