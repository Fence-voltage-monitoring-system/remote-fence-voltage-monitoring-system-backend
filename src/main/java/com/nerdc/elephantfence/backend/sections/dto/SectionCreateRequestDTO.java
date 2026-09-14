package com.nerdc.elephantfence.backend.sections.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionCreateRequestDTO {

    @NotNull(message = "Fence ID is required")
    private Long fenceId;

    @NotBlank(message = "Section code is required")
    @Size(max = 50, message = "Section code cannot exceed 50 characters")
    private String code;

    @Size(max = 100, message = "Start GPS cannot exceed 100 characters")
    private String startGps;

    @Size(max = 100, message = "End GPS cannot exceed 100 characters")
    private String endGps;

    @NotNull(message = "Length is required")
    @PositiveOrZero(message = "Length cannot be negative")
    private BigDecimal lengthKm;

    private Long provinceId;
    private Long districtId;
}
