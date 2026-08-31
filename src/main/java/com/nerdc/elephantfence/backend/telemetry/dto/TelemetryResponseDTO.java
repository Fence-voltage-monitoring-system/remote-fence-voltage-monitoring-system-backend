package com.nerdc.elephantfence.backend.telemetry.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelemetryResponseDTO {
    private String id;
    private String deviceSerial;
    private BigDecimal voltageKv;
    private Integer battery;
    private Integer signal;
    private OffsetDateTime recordedAt;
}
