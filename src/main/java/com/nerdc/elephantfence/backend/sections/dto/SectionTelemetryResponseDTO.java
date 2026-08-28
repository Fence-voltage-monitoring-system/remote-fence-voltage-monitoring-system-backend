package com.nerdc.elephantfence.backend.sections.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionTelemetryResponseDTO {
    private Long id;
    private Long deviceId;
    private String deviceName;
    private String deviceSerial;
    private BigDecimal voltageKv;
    private Integer battery;
    private Integer signal;
    private OffsetDateTime recordedAt;
}
