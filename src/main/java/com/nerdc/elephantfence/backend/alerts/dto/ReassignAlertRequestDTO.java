package com.nerdc.elephantfence.backend.alerts.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReassignAlertRequestDTO {
    @NotNull(message = "Staff ID is required")
    private Long staffId;

    private String reason;
}
