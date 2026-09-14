package com.nerdc.elephantfence.backend.alerts.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteWorkRequestDTO {
    @NotBlank(message = "Summary is required")
    private String summary;

    @NotBlank(message = "Cause is required")
    private String cause;

    @NotBlank(message = "Actions is required")
    private String actions;
}
