package com.nerdc.elephantfence.backend.reports.controller;

import com.nerdc.elephantfence.backend.reports.dto.HistoricalAnalysisResponseDTO;
import com.nerdc.elephantfence.backend.reports.service.HistoricalAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN', 'MAINTENANCE')")
public class HistoricalAnalysisController {

    private final HistoricalAnalysisService historicalAnalysisService;

    @GetMapping("/historical-analysis")
    public ResponseEntity<HistoricalAnalysisResponseDTO> getHistoricalAnalysis(
            @RequestParam(defaultValue = "24h") String period,
            @RequestParam(required = false) String deviceId
    ) {
        return ResponseEntity.ok(historicalAnalysisService.getAnalysis(period, deviceId));
    }
}
