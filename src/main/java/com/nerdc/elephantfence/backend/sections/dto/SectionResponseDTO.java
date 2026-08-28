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
public class SectionResponseDTO {

    private Long id;
    private Long fenceId;
    private String code;
    private String startGps;
    private String endGps;
    private BigDecimal lengthKm;
    private BigDecimal voltageKv;
    private Integer battery;
    private String status;
    private Long provinceId;
    private Long districtId;
    private OffsetDateTime updatedAt;
}
