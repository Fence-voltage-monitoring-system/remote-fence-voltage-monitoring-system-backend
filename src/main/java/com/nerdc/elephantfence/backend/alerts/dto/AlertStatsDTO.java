package com.nerdc.elephantfence.backend.alerts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertStatsDTO {
    private long activeCritical;
    private long activeWarnings;
    private long unacknowledged;
    private long underMaintenance;
    private long resolvedToday;
}
