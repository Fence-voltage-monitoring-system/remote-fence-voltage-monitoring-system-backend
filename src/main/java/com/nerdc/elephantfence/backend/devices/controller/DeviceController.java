package com.nerdc.elephantfence.backend.devices.controller;

import com.nerdc.elephantfence.backend.devices.dto.AssignDeviceRequestDTO;
import com.nerdc.elephantfence.backend.devices.dto.CreateDeviceRequestDTO;
import com.nerdc.elephantfence.backend.devices.dto.DeviceResponseDTO;
import com.nerdc.elephantfence.backend.devices.dto.UpdateDeviceRequestDTO;
import com.nerdc.elephantfence.backend.devices.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public ResponseEntity<List<DeviceResponseDTO>> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponseDTO> getDeviceById(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.getDeviceById(id));
    }

    @PostMapping
    public ResponseEntity<DeviceResponseDTO> createDevice(@Valid @RequestBody CreateDeviceRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deviceService.createDevice(dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DeviceResponseDTO> updateDevice(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeviceRequestDTO dto
    ) {
        return ResponseEntity.ok(deviceService.updateDevice(id, dto));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<DeviceResponseDTO> assignDevice(
            @PathVariable Long id,
            @Valid @RequestBody AssignDeviceRequestDTO dto
    ) {
        return ResponseEntity.ok(deviceService.assignDevice(id, dto));
    }

    @PostMapping("/{id}/unassign")
    public ResponseEntity<DeviceResponseDTO> unassignDevice(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.unassignDevice(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DeviceResponseDTO> toggleEnabled(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body
    ) {
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            throw new IllegalArgumentException("Field 'enabled' is required");
        }
        return ResponseEntity.ok(deviceService.toggleEnabled(id, enabled));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }
}
