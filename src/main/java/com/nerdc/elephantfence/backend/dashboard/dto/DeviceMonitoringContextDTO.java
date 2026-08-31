package com.nerdc.elephantfence.backend.dashboard.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceMonitoringContextDTO {
    private String fenceId;
    private String fenceName;
    private String sectionId;
    private String deviceId;
    private Double voltage;
    private Integer battery;
    private String status; // 'healthy' | 'warning' | 'critical' | 'offline'
}
