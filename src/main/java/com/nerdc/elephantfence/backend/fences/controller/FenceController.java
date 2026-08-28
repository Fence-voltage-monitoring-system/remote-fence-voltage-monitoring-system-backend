package com.nerdc.elephantfence.backend.fences.controller;

import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.fences.dto.FenceCreateRequestDTO;
import com.nerdc.elephantfence.backend.fences.dto.FenceResponseDTO;
import com.nerdc.elephantfence.backend.fences.dto.FenceUpdateRequestDTO;
import com.nerdc.elephantfence.backend.fences.dto.MaintenanceTeamRequestDTO;
import com.nerdc.elephantfence.backend.fences.service.FenceService;
import com.nerdc.elephantfence.backend.users.dto.UserResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fences")
@RequiredArgsConstructor
public class FenceController {

    private final FenceService fenceService;

    @GetMapping
    public ResponseEntity<List<FenceResponseDTO>> getAllFences(
            @RequestParam(required = false) Long provinceId,
            @RequestParam(required = false) Long districtId
    ) {
        return ResponseEntity.ok(fenceService.getAllFences(provinceId, districtId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FenceResponseDTO> getFenceById(@PathVariable Long id) {
        return ResponseEntity.ok(fenceService.getFenceById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<FenceResponseDTO> createFence(
            @Valid @RequestBody FenceCreateRequestDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fenceService.createFence(dto, principal));
    }

    @PostMapping("/drafts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<FenceResponseDTO> saveDraft(
            @Valid @RequestBody FenceCreateRequestDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.badRequest().body(fenceService.saveDraft(dto));
    }

    @GetMapping("/maintenance-candidates")
    public ResponseEntity<List<UserResponseDTO>> getMaintenanceCandidates(
            @RequestParam(required = false) Long fenceId,
            @RequestParam(required = false) Long provinceId,
            @RequestParam(required = false) Long districtId
    ) {
        return ResponseEntity.ok(fenceService.getMaintenanceCandidates(fenceId, provinceId, districtId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<FenceResponseDTO> updateFence(
            @PathVariable Long id,
            @Valid @RequestBody FenceUpdateRequestDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(fenceService.updateFence(id, dto, principal));
    }

    @PutMapping("/{id}/maintenance-team")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<FenceResponseDTO> assignMaintenanceTeam(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceTeamRequestDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(fenceService.assignMaintenanceTeam(id, dto, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<Void> deleteFence(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        fenceService.deleteFence(id, principal);
        return ResponseEntity.noContent().build();
    }
}
