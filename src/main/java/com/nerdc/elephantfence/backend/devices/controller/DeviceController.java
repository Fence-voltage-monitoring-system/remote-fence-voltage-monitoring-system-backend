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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN', 'MAINTENANCE')")
    public ResponseEntity<List<DeviceResponseDTO>> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN', 'MAINTENANCE')")
    public ResponseEntity<DeviceResponseDTO> getDeviceById(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.getDeviceById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<DeviceResponseDTO> createDevice(@Valid @RequestBody CreateDeviceRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deviceService.createDevice(dto));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<DeviceResponseDTO> updateDevice(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeviceRequestDTO dto
    ) {
        return ResponseEntity.ok(deviceService.updateDevice(id, dto));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<DeviceResponseDTO> assignDevice(
            @PathVariable Long id,
            @Valid @RequestBody AssignDeviceRequestDTO dto
    ) {
        return ResponseEntity.ok(deviceService.assignDevice(id, dto));
    }

    @PostMapping("/{id}/unassign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<DeviceResponseDTO> unassignDevice(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.unassignDevice(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
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
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }
}
