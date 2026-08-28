package com.nerdc.elephantfence.backend.configuration.controller;

import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.configuration.dto.ConfigurationSaveRequestDTO;
import com.nerdc.elephantfence.backend.configuration.dto.ConfigurationSaveResponseDTO;
import com.nerdc.elephantfence.backend.configuration.dto.SessionOverviewDTO;
import com.nerdc.elephantfence.backend.configuration.service.ConfigurationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/configuration")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class ConfigurationController {

    private final ConfigurationService configurationService;

    @GetMapping("/{section}")
    public ResponseEntity<ConfigurationSaveResponseDTO> getSection(@PathVariable String section) {
        return ResponseEntity.ok(configurationService.getSection(section));
    }

    @PutMapping("/{section}")
    public ResponseEntity<ConfigurationSaveResponseDTO> saveSection(
            @PathVariable String section,
            @Valid @RequestBody ConfigurationSaveRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(configurationService.saveSection(section, request, principal.getId()));
    }

    @GetMapping("/sessions/active")
    public ResponseEntity<SessionOverviewDTO> getSessionOverview() {
        return ResponseEntity.ok(configurationService.getSessionOverview());
    }

    @PostMapping("/sessions/active/{sessionId}/revoke")
    public ResponseEntity<Map<String, String>> revokeSession(@PathVariable String sessionId) {
        configurationService.revokeSession(sessionId);
        return ResponseEntity.ok(Map.of("message", "Session revoked successfully"));
    }

    @PostMapping("/sessions/users/{userId}/revoke")
    public ResponseEntity<Map<String, Object>> revokeUserSessions(@PathVariable UUID userId) {
        int count = configurationService.revokeUserSessions(userId);
        return ResponseEntity.ok(Map.of(
                "message", "Sessions revoked successfully",
                "revokedSessions", count
        ));
    }
}
