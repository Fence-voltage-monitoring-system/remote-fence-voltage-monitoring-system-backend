package com.nerdc.elephantfence.backend.devices.service;

import com.nerdc.elephantfence.backend.devices.dto.AssignDeviceRequestDTO;
import com.nerdc.elephantfence.backend.devices.dto.CreateDeviceRequestDTO;
import com.nerdc.elephantfence.backend.devices.dto.DeviceResponseDTO;
import com.nerdc.elephantfence.backend.devices.dto.UpdateDeviceRequestDTO;
import com.nerdc.elephantfence.backend.devices.entity.Device;
import com.nerdc.elephantfence.backend.devices.entity.DeviceStatus;
import com.nerdc.elephantfence.backend.devices.repository.DeviceRepository;
import com.nerdc.elephantfence.backend.gateways.repository.GatewayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private GatewayRepository gatewayRepository;

    @InjectMocks
    private DeviceService deviceService;

    private Device device;

    @BeforeEach
    void setUp() {
        device = Device.builder()
                .id(1L)
                .name("Fence Monitor 01")
                .serial("SN-2026-0001")
                .type("Voltage Monitor")
                .status(DeviceStatus.offline)
                .signal(0)
                .battery(0)
                .enabled(true)
                .build();
    }

    @Test
    void getAllDevices_shouldReturnList() {
        device.setFenceId(10L);
        device.setSectionId(20L);
        when(deviceRepository.findAll()).thenReturn(List.of(device));
        when(deviceRepository.findFenceNameById(10L)).thenReturn("Monaragala Elephant Protection Fence");
        when(deviceRepository.findSectionCodeById(20L)).thenReturn("SEC-001");

        List<DeviceResponseDTO> result = deviceService.getAllDevices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSerial()).isEqualTo("SN-2026-0001");
        assertThat(result.get(0).getFence()).isEqualTo("Monaragala Elephant Protection Fence");
        assertThat(result.get(0).getSection()).isEqualTo("SEC-001");
    }

    @Test
    void createDevice_shouldSaveDevice() {
        CreateDeviceRequestDTO dto = CreateDeviceRequestDTO.builder()
                .name("Fence Monitor 01")
                .serial("SN-2026-0001")
                .type("Voltage Monitor")
                .build();

        when(deviceRepository.existsBySerialIgnoreCase(dto.getSerial())).thenReturn(false);
        when(deviceRepository.save(any(Device.class))).thenReturn(device);

        DeviceResponseDTO result = deviceService.createDevice(dto);

        assertThat(result).isNotNull();
        assertThat(result.getSerial()).isEqualTo("SN-2026-0001");
        verify(deviceRepository, times(1)).save(any(Device.class));
    }

    @Test
    void createDevice_shouldThrowExceptionWhenSerialExists() {
        CreateDeviceRequestDTO dto = CreateDeviceRequestDTO.builder()
                .name("Fence Monitor 01")
                .serial("SN-2026-0001")
                .build();

        when(deviceRepository.existsBySerialIgnoreCase(dto.getSerial())).thenReturn(true);

        assertThatThrownBy(() -> deviceService.createDevice(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Device already exists with serial");
    }

    @Test
    void assignDevice_shouldAssignWhenValid() {
        AssignDeviceRequestDTO dto = AssignDeviceRequestDTO.builder()
                .fence("Monaragala Elephant Protection Fence")
                .section("SEC-001")
                .build();

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(deviceRepository.findFenceIdByName(dto.getFence())).thenReturn(100L);
        when(deviceRepository.findSectionIdByCodeAndFenceId(dto.getSection(), 100L)).thenReturn(200L);
        when(deviceRepository.save(any(Device.class))).thenReturn(device);

        DeviceResponseDTO result = deviceService.assignDevice(1L, dto);

        assertThat(result).isNotNull();
        verify(deviceRepository, times(1)).save(any(Device.class));
    }

    @Test
    void assignDevice_shouldThrowExceptionWhenFenceMissing() {
        AssignDeviceRequestDTO dto = AssignDeviceRequestDTO.builder()
                .fence("NonExistentFence")
                .section("SEC-001")
                .build();

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(deviceRepository.findFenceIdByName(dto.getFence())).thenReturn(null);

        assertThatThrownBy(() -> deviceService.assignDevice(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fence not found with name");
    }

    @Test
    void assignDevice_shouldThrowExceptionWhenSectionMissing() {
        AssignDeviceRequestDTO dto = AssignDeviceRequestDTO.builder()
                .fence("Monaragala Elephant Protection Fence")
                .section("NonExistentSection")
                .build();

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(deviceRepository.findFenceIdByName(dto.getFence())).thenReturn(100L);
        when(deviceRepository.findSectionIdByCodeAndFenceId(dto.getSection(), 100L)).thenReturn(null);

        assertThatThrownBy(() -> deviceService.assignDevice(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Section not found with code");
    }

    @Test
    void unassignDevice_shouldClearSectionAndFence() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenReturn(device);

        DeviceResponseDTO result = deviceService.unassignDevice(1L);

        assertThat(result).isNotNull();
        verify(deviceRepository, times(1)).save(any(Device.class));
    }

    @Test
    void deleteDevice_shouldDelete() {
        when(deviceRepository.existsById(1L)).thenReturn(true);

        deviceService.deleteDevice(1L);

        verify(deviceRepository, times(1)).deleteById(1L);
    }
}
