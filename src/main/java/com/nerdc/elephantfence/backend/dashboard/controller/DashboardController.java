package com.nerdc.elephantfence.backend.dashboard.controller;

import com.nerdc.elephantfence.backend.dashboard.dto.DashboardOverviewResponseDTO;
import com.nerdc.elephantfence.backend.dashboard.dto.DeviceAnalyticsResponseDTO;
import com.nerdc.elephantfence.backend.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
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
