package com.nerdc.elephantfence.backend.dashboard.controller;

import com.nerdc.elephantfence.backend.dashboard.dto.DashboardOverviewResponseDTO;
import com.nerdc.elephantfence.backend.dashboard.dto.DeviceAnalyticsResponseDTO;
import com.nerdc.elephantfence.backend.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN', 'MAINTENANCE')")
public class DashboardController {

    private final DashboardService dashboardService;

    // GET /api/dashboard
    @GetMapping
    public ResponseEntity<DashboardOverviewResponseDTO> getOverview() {
        return ResponseEntity.ok(dashboardService.getOverview());
    }

    // GET /api/dashboard/devices/{id}
    @GetMapping("/devices/{id}")
    public ResponseEntity<DeviceAnalyticsResponseDTO> getDeviceAnalytics(@PathVariable String id) {
        return ResponseEntity.ok(dashboardService.getDeviceAnalytics(id));
    }
}
