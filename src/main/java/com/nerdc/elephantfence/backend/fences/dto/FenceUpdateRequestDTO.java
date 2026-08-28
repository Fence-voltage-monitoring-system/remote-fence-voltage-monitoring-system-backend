package com.nerdc.elephantfence.backend.fences.dto;

import jakarta.validation.constraints.Positive;
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
public class FenceUpdateRequestDTO {
    @Size(max = 50, message = "Fence code cannot exceed 50 characters")
    private String code;

    @Size(max = 150, message = "Fence name cannot exceed 150 characters")
    private String name;

    private Long provinceId;
    private Long districtId;

    @Positive(message = "Length must be greater than zero")
    private BigDecimal lengthKm;

    private String health;
}
