package com.nerdc.elephantfence.backend.alerts.controller;

import com.nerdc.elephantfence.backend.alerts.dto.*;
import com.nerdc.elephantfence.backend.alerts.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<AlertPageDTO> getAlerts(
            @ModelAttribute AlertFilters filters,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ResponseEntity.ok(alertService.getAlerts(filters, page, pageSize));
    }

    @GetMapping("/stats")
    public ResponseEntity<AlertStatsDTO> getStats() {
        return ResponseEntity.ok(alertService.getStats());
    }

    @GetMapping("/{id}/eligible-maintenance")
    public ResponseEntity<List<MaintenanceStaffOptionDTO>> getEligibleMaintenance(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.getEligibleMaintenance(id));
    }

    @PatchMapping("/{id}/acknowledge")
    public ResponseEntity<AlertResponseDTO> acknowledge(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.acknowledge(id));
    }

    @PatchMapping("/{id}/maintenance-assignment/accept")
    public ResponseEntity<AlertResponseDTO> acceptAssignment(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.acceptAssignment(id));
    }

    @PatchMapping("/{id}/maintenance-assignment/decline")
    public ResponseEntity<AlertResponseDTO> declineAssignment(
            @PathVariable Long id,
            @Valid @RequestBody DeclineAssignmentRequestDTO dto
    ) {
        return ResponseEntity.ok(alertService.declineAssignment(id, dto));
    }

    @PostMapping("/{id}/maintenance-assignments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN')")
    public ResponseEntity<AlertResponseDTO> reassignMaintenance(
            @PathVariable Long id,
            @Valid @RequestBody ReassignAlertRequestDTO dto
    ) {
        return ResponseEntity.ok(alertService.reassignMaintenance(id, dto));
    }

    @PostMapping("/{id}/maintenance-assignment/escalate")
    public ResponseEntity<AlertResponseDTO> escalateAssignment(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.escalateAssignment(id));
    }

    @PatchMapping("/{id}/work/start")
    public ResponseEntity<AlertResponseDTO> startWork(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.startWork(id));
    }

    @PatchMapping("/{id}/work/complete")
    public ResponseEntity<AlertResponseDTO> completeWork(
            @PathVariable Long id,
            @Valid @RequestBody CompleteWorkRequestDTO dto
    ) {
        return ResponseEntity.ok(alertService.completeWork(id, dto));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN')")
    public ResponseEntity<AlertResponseDTO> resolveManually(
            @PathVariable Long id,
            @Valid @RequestBody ResolveManuallyRequestDTO dto
    ) {
        return ResponseEntity.ok(alertService.resolveManually(id, dto));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<AlertResponseDTO> addComment(
            @PathVariable Long id,
            @Valid @RequestBody AddCommentRequestDTO dto
    ) {
        return ResponseEntity.ok(alertService.addComment(id, dto));
    }
}
