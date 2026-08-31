package com.nerdc.elephantfence.backend.dashboard.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDTO {
    private long totalFences;
    private long totalDevices;
    private long activeDevices;
    private long criticalAlerts;
    private long lowVoltageFences;
}
