package com.nerdc.elephantfence.backend.systemhealth.controller;

import com.nerdc.elephantfence.backend.systemhealth.dto.JobRetryResponseDTO;
import com.nerdc.elephantfence.backend.systemhealth.dto.SystemHealthSnapshotDTO;
import com.nerdc.elephantfence.backend.systemhealth.service.SystemHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/system-health")
@RequiredArgsConstructor
public class SystemHealthController {

    private final SystemHealthService systemHealthService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'MAINTENANCE')")
    public ResponseEntity<SystemHealthSnapshotDTO> getSnapshot() {
        return ResponseEntity.ok(systemHealthService.getSnapshot());
    }

    @PostMapping("/jobs/{id}/retry")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MAINTENANCE')")
    public ResponseEntity<JobRetryResponseDTO> retryJob(
            @PathVariable String id,
            @RequestBody Map<String, String> payload) {
        String reason = payload.getOrDefault("reason", "Manual retry");
        return ResponseEntity.ok(systemHealthService.retryJob(id, reason));
    }
}
