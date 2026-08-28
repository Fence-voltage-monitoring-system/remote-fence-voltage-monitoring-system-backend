package com.nerdc.elephantfence.backend.sections.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionBulkRowDTO {
    private Double startLatitude;
    private Double startLongitude;
    private Double endLatitude;
    private Double endLongitude;

    @NotNull(message = "Length is required")
    @PositiveOrZero(message = "Length cannot be negative")
    private BigDecimal lengthKm;

    private String installationDate;
    private String notes;
}
