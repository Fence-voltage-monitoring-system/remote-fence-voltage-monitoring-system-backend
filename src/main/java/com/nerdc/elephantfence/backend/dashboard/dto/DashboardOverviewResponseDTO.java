package com.nerdc.elephantfence.backend.dashboard.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardOverviewResponseDTO {
    private DashboardSummaryDTO summary;
    private DeviceMonitoringContextDTO selectedDevice;
}
