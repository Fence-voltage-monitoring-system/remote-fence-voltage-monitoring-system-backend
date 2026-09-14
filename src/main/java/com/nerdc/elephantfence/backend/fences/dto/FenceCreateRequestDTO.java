package com.nerdc.elephantfence.backend.fences.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class FenceCreateRequestDTO {
    @NotBlank(message = "Fence code is required")
    @Size(max = 50, message = "Fence code cannot exceed 50 characters")
    private String code;

    @NotBlank(message = "Fence name is required")
    @Size(max = 150, message = "Fence name cannot exceed 150 characters")
    private String name;

    @NotNull(message = "Province ID is required")
    private Long provinceId;

    @NotNull(message = "District ID is required")
    private Long districtId;

    @NotNull(message = "Length is required")
    @Positive(message = "Length must be greater than zero")
    private BigDecimal lengthKm;
    
    private String health; // Optional, defaults to OFFLINE
}
