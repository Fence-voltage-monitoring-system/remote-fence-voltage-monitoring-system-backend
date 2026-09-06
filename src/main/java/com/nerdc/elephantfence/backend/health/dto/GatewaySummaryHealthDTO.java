package com.nerdc.elephantfence.backend.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewaySummaryHealthDTO {
    private Integer total;
    private Integer online;
    private Integer offline;
    private Integer lateReporting;
    private Double communicationSuccessPercent;
    private OffsetDateTime latestTelemetryAt;
}
