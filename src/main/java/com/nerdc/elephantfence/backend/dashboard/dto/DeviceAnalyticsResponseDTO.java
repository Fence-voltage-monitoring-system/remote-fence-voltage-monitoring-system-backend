package com.nerdc.elephantfence.backend.dashboard.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceAnalyticsResponseDTO {
    private DeviceMonitoringContextDTO device;
    private List<VoltageReadingDTO> voltageHistory;
    private AlertCountsDTO alertCounts;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlertCountsDTO {
        private long critical;
        private long warning;
        private long offline;
        private long resolved;
    }
}
