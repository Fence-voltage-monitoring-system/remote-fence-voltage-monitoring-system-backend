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
    private java.util.UUID staffId;

    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max=2000)
    private String reason;
}
