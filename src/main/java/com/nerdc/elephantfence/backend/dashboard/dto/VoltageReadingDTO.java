package com.nerdc.elephantfence.backend.dashboard.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoltageReadingDTO {
    private OffsetDateTime recordedAt;
    private Double voltage;
}
