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
public class UnhealthyGatewayHealthDTO {
    private Long id;
    private String code;
    private String fenceCode;
    private String state; // OFFLINE, LATE
    private OffsetDateTime lastCommunicationAt;
    private OffsetDateTime nextExpectedAt;
    private Integer delayMinutes;
}
