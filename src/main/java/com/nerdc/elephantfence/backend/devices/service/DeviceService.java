package com.nerdc.elephantfence.backend.devices.service;

import com.nerdc.elephantfence.backend.devices.dto.AssignDeviceRequestDTO;
import com.nerdc.elephantfence.backend.devices.dto.CreateDeviceRequestDTO;
import com.nerdc.elephantfence.backend.devices.dto.DeviceResponseDTO;
import com.nerdc.elephantfence.backend.devices.dto.UpdateDeviceRequestDTO;
import com.nerdc.elephantfence.backend.devices.entity.Device;
import com.nerdc.elephantfence.backend.devices.entity.DeviceStatus;
import com.nerdc.elephantfence.backend.devices.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;

    @Transactional(readOnly = true)
    public List<DeviceResponseDTO> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeviceResponseDTO getDeviceById(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with ID: " + id));
        return convertToResponseDTO(device);
    }

    @Transactional
    public DeviceResponseDTO createDevice(CreateDeviceRequestDTO dto) {
        if (deviceRepository.existsBySerialIgnoreCase(dto.getSerial())) {
            throw new IllegalArgumentException("Device already exists with serial: " + dto.getSerial());
        }

        String type = dto.getType() != null && !dto.getType().isBlank() ? dto.getType() : "Voltage Monitor";

        Device.DeviceBuilder deviceBuilder = Device.builder()
                .name(dto.getName())
                .serial(dto.getSerial().trim())
                .type(type)
                .status(DeviceStatus.offline)
                .signal(0)
                .battery(0)
                .enabled(true);

        if (dto.getFence() != null && !dto.getFence().isBlank()) {
            Long fenceId = deviceRepository.findFenceIdByName(dto.getFence().trim());
            if (fenceId != null) {
                deviceBuilder.fenceId(fenceId);
                if (dto.getSection() != null && !dto.getSection().isBlank()) {
                    Long sectionId = deviceRepository.findSectionIdByCodeAndFenceId(dto.getSection().trim(), fenceId);
                    if (sectionId != null) {
                        deviceBuilder.sectionId(sectionId);
                        deviceBuilder.status(DeviceStatus.online); // assign means online
                        deviceBuilder.voltage(6.0); // default voltage
                        deviceBuilder.signal(100);
                    }
                }
            }
        }

        Device saved = deviceRepository.save(deviceBuilder.build());
        return convertToResponseDTO(saved);
    }

    @Transactional
    public DeviceResponseDTO updateDevice(Long id, UpdateDeviceRequestDTO dto) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with ID: " + id));

        if (dto.getName() != null) device.setName(dto.getName());
        if (dto.getType() != null) device.setType(dto.getType());
        if (dto.getEnabled() != null) device.setEnabled(dto.getEnabled());

        if (dto.getFence() != null) {
            if (dto.getFence().isBlank()) {
                device.setFenceId(null);
                device.setSectionId(null);
            } else {
                Long fenceId = deviceRepository.findFenceIdByName(dto.getFence().trim());
                if (fenceId == null) {
                    throw new IllegalArgumentException("Fence not found with name: " + dto.getFence());
                }
                device.setFenceId(fenceId);

                if (dto.getSection() != null) {
                    if (dto.getSection().isBlank()) {
                        device.setSectionId(null);
                    } else {
                        Long sectionId = deviceRepository.findSectionIdByCodeAndFenceId(dto.getSection().trim(), fenceId);
                        if (sectionId == null) {
                            throw new IllegalArgumentException("Section not found with code: " + dto.getSection() + " on fence: " + dto.getFence());
                        }
                        device.setSectionId(sectionId);
                    }
                }
            }
        }

        Device updated = deviceRepository.save(device);
        return convertToResponseDTO(updated);
    }

    @Transactional
    public DeviceResponseDTO assignDevice(Long id, AssignDeviceRequestDTO dto) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with ID: " + id));

        Long fenceId = deviceRepository.findFenceIdByName(dto.getFence().trim());
        if (fenceId == null) {
            throw new IllegalArgumentException("Fence not found with name: " + dto.getFence());
        }

        Long sectionId = deviceRepository.findSectionIdByCodeAndFenceId(dto.getSection().trim(), fenceId);
        if (sectionId == null) {
            throw new IllegalArgumentException("Section not found with code: " + dto.getSection() + " on fence: " + dto.getFence());
        }

        device.setFenceId(fenceId);
        device.setSectionId(sectionId);
        device.setStatus(DeviceStatus.online);
        device.setVoltage(6.0);
        device.setSignal(100);

        Device updated = deviceRepository.save(device);
        return convertToResponseDTO(updated);
    }

    @Transactional
    public DeviceResponseDTO unassignDevice(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with ID: " + id));

        device.setFenceId(null);
        device.setSectionId(null);
        device.setStatus(DeviceStatus.offline);
        device.setVoltage(null);
        device.setSignal(0);

        Device updated = deviceRepository.save(device);
        return convertToResponseDTO(updated);
    }

    @Transactional
    public DeviceResponseDTO toggleEnabled(Long id, boolean enabled) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with ID: " + id));
        device.setEnabled(enabled);
        Device updated = deviceRepository.save(device);
        return convertToResponseDTO(updated);
    }

    @Transactional
    public void deleteDevice(Long id) {
        if (!deviceRepository.existsById(id)) {
            throw new IllegalArgumentException("Device not found with ID: " + id);
        }
        deviceRepository.deleteById(id);
    }

    private DeviceResponseDTO convertToResponseDTO(Device device) {
        String fenceName = null;
        if (device.getFenceId() != null) {
            fenceName = deviceRepository.findFenceNameById(device.getFenceId());
        }

        String sectionCode = null;
        if (device.getSectionId() != null) {
            sectionCode = deviceRepository.findSectionCodeById(device.getSectionId());
        }

        return DeviceResponseDTO.builder()
                .id(device.getId().toString())
                .name(device.getName())
                .serial(device.getSerial())
                .type(device.getType())
                .status(device.getStatus().name())
                .voltage(device.getVoltage())
                .signal(device.getSignal())
                .battery(device.getBattery())
                .enabled(device.isEnabled())
                .lastSeen(device.getLastSeen())
                .fence(fenceName)
                .section(sectionCode)
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .build();
    }
}
