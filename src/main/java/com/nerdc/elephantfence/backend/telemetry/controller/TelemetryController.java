package com.nerdc.elephantfence.backend.telemetry.controller;

import com.nerdc.elephantfence.backend.telemetry.dto.TelemetryIngestRequestDTO;
import com.nerdc.elephantfence.backend.telemetry.dto.TelemetryResponseDTO;
import com.nerdc.elephantfence.backend.telemetry.service.TelemetryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryService telemetryService;

    @PostMapping("/ingest")
    public ResponseEntity<TelemetryResponseDTO> ingestTelemetry(@Valid @RequestBody TelemetryIngestRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(telemetryService.ingestTelemetry(dto));
    }

    @GetMapping("/device/{deviceId}/history")
    public ResponseEntity<List<TelemetryResponseDTO>> getDeviceHistory(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(telemetryService.getDeviceHistory(deviceId, limit));
    }
}
