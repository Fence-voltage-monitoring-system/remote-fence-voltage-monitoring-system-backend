package com.nerdc.elephantfence.backend.sections.controller;

import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.sections.dto.SectionCreateRequestDTO;
import com.nerdc.elephantfence.backend.sections.dto.SectionResponseDTO;
import com.nerdc.elephantfence.backend.sections.dto.SectionUpdateRequestDTO;
import com.nerdc.elephantfence.backend.sections.dto.SectionTelemetryResponseDTO;
import com.nerdc.elephantfence.backend.sections.dto.SectionBulkImportRequestDTO;
import com.nerdc.elephantfence.backend.sections.service.SectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionService sectionService;

    @GetMapping
    public ResponseEntity<List<SectionResponseDTO>> getByFenceId(@RequestParam Long fenceId) {
        return ResponseEntity.ok(sectionService.getByFenceId(fenceId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SectionResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sectionService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<SectionResponseDTO> create(
            @Valid @RequestBody SectionCreateRequestDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sectionService.create(dto, principal));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<List<SectionResponseDTO>> createBulk(
            @Valid @RequestBody SectionBulkImportRequestDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sectionService.bulkCreate(dto, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<SectionResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody SectionUpdateRequestDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(sectionService.update(id, dto, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        sectionService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/telemetry")
    public ResponseEntity<List<SectionTelemetryResponseDTO>> getTelemetry(
            @PathVariable Long id,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(sectionService.getSectionTelemetry(id, limit));
    }
}
