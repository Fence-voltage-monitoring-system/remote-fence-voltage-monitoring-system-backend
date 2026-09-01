package com.nerdc.elephantfence.backend.gateways.controller;

import com.nerdc.elephantfence.backend.gateways.dto.CreateGatewayRequestDTO;
import com.nerdc.elephantfence.backend.gateways.dto.GatewayResponseDTO;
import com.nerdc.elephantfence.backend.gateways.dto.UpdateGatewayRequestDTO;
import com.nerdc.elephantfence.backend.gateways.service.GatewayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/gateways")
@RequiredArgsConstructor
public class GatewayController {

    private final GatewayService gatewayService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN', 'MAINTENANCE')")
    public ResponseEntity<List<GatewayResponseDTO>> getAllGateways() {
        return ResponseEntity.ok(gatewayService.getAllGateways());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN', 'MAINTENANCE')")
    public ResponseEntity<GatewayResponseDTO> getGatewayById(@PathVariable Long id) {
        return ResponseEntity.ok(gatewayService.getGatewayById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN')")
    public ResponseEntity<GatewayResponseDTO> createGateway(@Valid @RequestBody CreateGatewayRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gatewayService.createGateway(dto));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN')")
    public ResponseEntity<GatewayResponseDTO> updateGateway(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGatewayRequestDTO dto
    ) {
        return ResponseEntity.ok(gatewayService.updateGateway(id, dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN')")
    public ResponseEntity<GatewayResponseDTO> toggleEnabled(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body
    ) {
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            throw new IllegalArgumentException("Field 'enabled' is required");
        }
        return ResponseEntity.ok(gatewayService.toggleEnabled(id, enabled));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteGateway(@PathVariable Long id) {
        gatewayService.deleteGateway(id);
        return ResponseEntity.noContent().build();
    }
}
