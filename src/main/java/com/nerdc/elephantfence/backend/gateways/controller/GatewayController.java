package com.nerdc.elephantfence.backend.gateways.controller;

import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.gateways.dto.*;
import com.nerdc.elephantfence.backend.gateways.service.GatewayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/gateways")
@RequiredArgsConstructor
public class GatewayController {

    private final GatewayService gatewayService;

    @GetMapping
    public ResponseEntity<List<GatewayResponseDTO>> getAllGateways() {
        return ResponseEntity.ok(gatewayService.getAllGateways());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<GatewayResponseDTO> createGateway(
            @Valid @RequestBody GatewayCreateRequestDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gatewayService.createGateway(dto, principal));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<GatewayResponseDTO> updateGateway(
            @PathVariable String id,
            @Valid @RequestBody GatewayUpdateRequestDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(gatewayService.updateGateway(id, dto, principal));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<GatewayResponseDTO> toggleEnabled(
            @PathVariable String id,
            @RequestBody Map<String, Boolean> payload,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Boolean enabled = payload.get("enabled");
        if (enabled == null) {
            throw new IllegalArgumentException("Field 'enabled' is required");
        }
        return ResponseEntity.ok(gatewayService.toggleEnabled(id, enabled, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<Void> deleteGateway(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        gatewayService.deleteGateway(id, principal);
        return ResponseEntity.noContent().build();
    }
}
