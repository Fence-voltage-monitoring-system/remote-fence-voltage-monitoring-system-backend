package com.nerdc.elephantfence.backend.health.controller;

import com.nerdc.elephantfence.backend.health.dto.SystemHealthSnapshotDTO;
import com.nerdc.elephantfence.backend.health.dto.RetryRequestDTO;
import com.nerdc.elephantfence.backend.health.service.SystemHealthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/system-health")
@RequiredArgsConstructor
public class SystemHealthController {

    private final SystemHealthService healthService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN')")
    public ResponseEntity<SystemHealthSnapshotDTO> getSnapshot() {
        return ResponseEntity.ok(healthService.getHealthSnapshot());
    }

    @PostMapping("/jobs/{jobId}/retry")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN')")
    public ResponseEntity<Map<String, String>> retryJob(
            @PathVariable String jobId,
            @RequestBody @Valid RetryRequestDTO request
    ) {
        String executionId = healthService.retryBackgroundJob(jobId, request.getReason());
        return ResponseEntity.ok(Map.of(
                "message", "Job execution queued successfully",
                "executionId", executionId
        ));
    }
}
